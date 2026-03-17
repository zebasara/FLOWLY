package com.flowly.move.data.repository

import android.content.Context
import android.net.Uri
import com.flowly.move.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FlowlyRepository(private val context: Context) {

    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ── Límites diarios ──────────────────────────────────────────────────
    companion object {
        // Tasa: 1 MOVE cada 100m (antes era 1/200m) → 10 MOVE/km
        // Con 5 km caminados: 50 MOVE; con 10 km: 100 MOVE
        const val METROS_POR_MOVE        = 100   // metros necesarios para ganar 1 MOVE
        const val DAILY_LIMIT_MOVIMIENTO = 500   // MOVE máx por día caminando/corriendo
        const val DAILY_LIMIT_VIDEOS     = 200   // MOVE máx por día viendo videos (4 videos × 50)
        // Máximo teórico: 700 MOVE/día → saldo mínimo nivel 1 (15.000) en ~21 días
    }

    /** Devuelve "yyyy-MM-dd" del día de hoy en zona local */
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /**
     * Verifica si los contadores diarios del usuario necesitan resetearse.
     * Si la fecha guardada ≠ hoy, resetea tokenMovimientoHoy, tokenVideosHoy y kmHoy.
     * Retorna el usuario actualizado (con los contadores ya reseteados si corresponde).
     */
    private suspend fun checkAndResetDaily(uid: String, user: com.flowly.move.data.model.User): com.flowly.move.data.model.User {
        val todayStr = today()
        if (user.lastTokenResetDate == todayStr) return user   // ya está en el día de hoy
        // Es un día nuevo → resetear contadores diarios
        userRef(uid).update(
            mapOf(
                "tokenMovimientoHoy"     to 0,
                "tokenVideosHoy"         to 0,
                "kmHoy"                  to 0f,
                "lastTokenResetDate"     to todayStr,
                "misionesReclamadasHoy"  to emptyList<String>()
            )
        ).await()
        return user.copy(
            tokenMovimientoHoy    = 0,
            tokenVideosHoy        = 0,
            kmHoy                 = 0f,
            lastTokenResetDate    = todayStr,
            misionesReclamadasHoy = emptyList()
        )
    }

    private fun userRef(uid: String) = db.collection("usuarios").document(uid)
    private fun canjesRef(uid: String) = userRef(uid).collection("canjes")
    private fun holdingsRef(uid: String) = userRef(uid).collection("holdings")
    private fun notifsRef(uid: String) = userRef(uid).collection("notificaciones")

    // ── Usuario ──────────────────────────────────────────────────

    suspend fun getUser(uid: String): Result<User?> = runCatching {
        userRef(uid).get().await().toObject(User::class.java)
    }

    // ── Video tokens ─────────────────────────────────────────────

    suspend fun cobrarVideo(uid: String, amount: Int = 50): Result<Unit> = runCatching {
        userRef(uid).update(
            mapOf(
                "tokensActuales" to FieldValue.increment(amount.toLong()),
                "tokenVideosHoy" to FieldValue.increment(amount.toLong())
            )
        ).await()
        // Crear notificación de video
        val notif = Notificacion(
            uid    = uid,
            titulo = "Video completado 🎬",
            cuerpo = "+$amount MOVE acreditados",
            tipo   = "video",
            createdAt = System.currentTimeMillis()
        )
        crearNotificacion(uid, notif)
    }

    // ── Canjes ───────────────────────────────────────────────────

    suspend fun getCanjes(uid: String): Result<List<Canje>> = runCatching {
        canjesRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Canje::class.java)
    }

    suspend fun createCanje(uid: String, canje: Canje): Result<Unit> = runCatching {
        val ref = canjesRef(uid).document()
        val finalCanje = canje.copy(id = ref.id, uid = uid, createdAt = System.currentTimeMillis())
        val batch = db.batch()
        batch.set(ref, finalCanje)
        batch.update(
            userRef(uid),
            "tokensActuales", FieldValue.increment(-canje.moveDescontado.toLong())
        )
        batch.commit().await()
        // Notificación de canje
        crearNotificacion(uid, Notificacion(
            uid    = uid,
            titulo = "Canje solicitado 💸",
            cuerpo = "${canje.montoLabel} → ${canje.aliasDestino} · en proceso",
            tipo   = "pago",
            createdAt = System.currentTimeMillis()
        ))
    }

    // ── Holdings ─────────────────────────────────────────────────

    suspend fun getHoldings(uid: String): Result<List<Holding>> = runCatching {
        holdingsRef(uid)
            .orderBy("fechaInicio", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Holding::class.java)
    }

    suspend fun createHolding(uid: String, holding: Holding): Result<Unit> = runCatching {
        val ref = holdingsRef(uid).document()
        val finalHolding = holding.copy(id = ref.id, uid = uid)
        val batch = db.batch()
        batch.set(ref, finalHolding)
        batch.update(
            userRef(uid),
            mapOf(
                "tokensActuales" to FieldValue.increment(-holding.moveAmount.toLong()),
                "moveEnHolding"  to FieldValue.increment(holding.moveAmount.toLong())
            )
        )
        batch.commit().await()
    }

    // ── Rankings ─────────────────────────────────────────────────

    suspend fun getRankings(
        scope: String,
        ciudad: String = "",
        provincia: String = ""
    ): Result<List<User>> = runCatching {
        val base = db.collection("usuarios")
            .orderBy("tokensActuales", Query.Direction.DESCENDING)
            .limit(200)
            .get().await()
            .toObjects(User::class.java)
        when (scope) {
            "ciudad"    -> base.filter { it.ciudad.equals(ciudad, ignoreCase = true) }.take(50)
            "provincia" -> base.filter { it.provincia.equals(provincia, ignoreCase = true) }.take(50)
            else        -> base.take(50)
        }
    }

    // ── Notificaciones ───────────────────────────────────────────

    suspend fun getNotificaciones(uid: String): Result<List<Notificacion>> = runCatching {
        notifsRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Notificacion::class.java)
    }

    suspend fun marcarLeida(uid: String, notifId: String): Result<Unit> = runCatching {
        notifsRef(uid).document(notifId).update("leida", true).await()
    }

    private suspend fun crearNotificacion(uid: String, notif: Notificacion) {
        runCatching {
            val ref = notifsRef(uid).document()
            ref.set(notif.copy(id = ref.id)).await()
        }
    }

    // ── Referidos ────────────────────────────────────────────────

    suspend fun registrarReferido(uidReferidor: String): Result<Unit> = runCatching {
        userRef(uidReferidor).update(
            mapOf(
                "totalReferidos" to FieldValue.increment(1),
                "tokensActuales" to FieldValue.increment(200L)
            )
        ).await()
    }

    // Busca el uid por código de referido (los primeros 8 chars del uid)
    suspend fun findUidByReferralCode(code: String): Result<String?> = runCatching {
        if (code.isBlank()) return@runCatching null
        // El código de referido son los primeros 8 chars del uid
        // Buscamos usuarios donde uid empiece con ese código
        val snap = db.collection("usuarios")
            .whereGreaterThanOrEqualTo("uid", code)
            .whereLessThan("uid", code + "\uF7FF")
            .limit(1)
            .get().await()
        snap.documents.firstOrNull()?.id
    }

    // ── Insignias ────────────────────────────────────────────────

    suspend fun otorgarInsignia(uid: String, badgeId: String): Result<Unit> = runCatching {
        userRef(uid).update("badges", FieldValue.arrayUnion(badgeId)).await()
    }

    // ── Perfil / Foto ────────────────────────────────────────────

    suspend fun updateProfile(
        uid: String,
        nombre: String,
        telefono: String,
        provincia: String,
        ciudad: String,
        alias: String
    ): Result<Unit> = runCatching {
        userRef(uid).update(
            mapOf(
                "nombre"           to nombre.trim(),
                "telefono"         to telefono.trim(),
                "provincia"        to provincia.trim(),
                "ciudad"           to ciudad.trim(),
                "aliasMercadoPago" to alias.trim()
            )
        ).await()
    }

    suspend fun uploadProfilePhoto(uid: String, photoUri: Uri): Result<String> = runCatching {
        val ref   = storage.reference.child("profile_photos/$uid.jpg")
        ref.putFile(photoUri).await()
        val url = ref.downloadUrl.await().toString()
        userRef(uid).update("profilePhotoUrl", url).await()
        url
    }

    // ── Tienda (config en Firestore) ─────────────────────────────

    suspend fun getStoreConfig(): Result<StoreConfig> = runCatching {
        val snap = db.collection("config").document("store").get().await()
        if (!snap.exists()) return@runCatching StoreConfig(
            umbralUsuarios = 500,
            productos      = DEFAULT_STORE_PRODUCTS
        )
        val umbral          = (snap.getLong("umbralUsuarios") ?: 500L).toInt()
        val referralBaseUrl = snap.getString("referralBaseUrl") ?: "https://flowly.app/r/"
        @Suppress("UNCHECKED_CAST")
        val rawProds  = snap.get("productos") as? List<Map<String, Any>> ?: emptyList()
        val productos = rawProds.map { m ->
            StoreProduct(
                id            = m["id"] as? String ?: "",
                nombre        = m["nombre"] as? String ?: "",
                descripcion   = m["descripcion"] as? String ?: "",
                moveRequerido = (m["moveRequerido"] as? Long)?.toInt() ?: 0,
                montoLabel    = m["montoLabel"] as? String ?: "",
                categoria     = m["categoria"] as? String ?: "cash",
                activo        = m["activo"] as? Boolean ?: true,
                imagenUrl     = m["imagenUrl"] as? String ?: ""
            )
        }
        val mercadoPagoUrl = snap.getString("mercadoPagoUrl") ?: ""
        StoreConfig(
            umbralUsuarios  = umbral,
            productos       = if (productos.isEmpty()) DEFAULT_STORE_PRODUCTS else productos,
            referralBaseUrl = referralBaseUrl,
            mercadoPagoUrl  = mercadoPagoUrl
        )
    }

    suspend fun getUserCount(): Result<Long> = runCatching {
        try {
            db.collection("usuarios").count().get(
                com.google.firebase.firestore.AggregateSource.SERVER
            ).await().count
        } catch (_: Exception) {
            // Fallback: recuento local si el API Aggregate no está disponible o falla
            db.collection("usuarios").get().await().size().toLong()
        }
    }

    // ── Sesión de movimiento (GPS) ───────────────────────────────────

    /**
     * Guarda los resultados de una sesión MOVErme:
     * - Incrementa kmHoy / kmTotales
     * - Acredita MOVE (1 MOVE por 200m, máx 200/día)
     * - Otorga insignias de distancia si corresponde
     */
    suspend fun registrarSesionMovimiento(uid: String, distanceMeters: Float): Result<Unit> = runCatching {
        if (distanceMeters < 50f) return@runCatching // ignorar < 50m

        // Leer usuario y resetear diario si es un nuevo día
        var user = getUser(uid).getOrNull() ?: return@runCatching
        user = checkAndResetDaily(uid, user)

        // Verificar límite diario de movimiento
        val yaGanado = user.tokenMovimientoHoy
        if (yaGanado >= DAILY_LIMIT_MOVIMIENTO) return@runCatching  // límite alcanzado

        val distKm        = distanceMeters / 1000f
        val movePorSesion = (distanceMeters / METROS_POR_MOVE).toInt()
        // No superar el límite diario restante
        val moveEarned    = movePorSesion.coerceAtMost(DAILY_LIMIT_MOVIMIENTO - yaGanado)

        userRef(uid).update(
            mapOf(
                "kmHoy"              to FieldValue.increment(distKm.toDouble()),
                "kmTotales"          to FieldValue.increment(distKm.toDouble()),
                "tokenMovimientoHoy" to FieldValue.increment(moveEarned.toLong()),
                "tokensActuales"     to FieldValue.increment(moveEarned.toLong())
            )
        ).await()

        // Notificación de sesión
        if (moveEarned > 0) {
            val limiteMensaje = if (yaGanado + moveEarned >= DAILY_LIMIT_MOVIMIENTO)
                " · límite diario alcanzado" else ""
            crearNotificacion(uid, Notificacion(
                uid       = uid,
                titulo    = "¡Sesión MOVErme completada! 🏃",
                cuerpo    = "+$moveEarned MOVE · ${"%.1f".format(distKm)} km recorridos$limiteMensaje",
                tipo      = "movimiento",
                createdAt = System.currentTimeMillis()
            ))
        }

        // Verificar badges de distancia
        val userActualizado = getUser(uid).getOrNull() ?: return@runCatching
        val kmNuevos = userActualizado.kmTotales + distKm
        val badges   = userActualizado.badges
        if (kmNuevos >= 200 && "maratonista"          !in badges) otorgarInsignia(uid, "maratonista")
        else if (kmNuevos >= 100 && "100km"           !in badges) otorgarInsignia(uid, "100km")
        else if (kmNuevos >= 50  && "gran_explorador" !in badges) otorgarInsignia(uid, "gran_explorador")
        else if (kmNuevos >= 5   && "explorador"      !in badges) otorgarInsignia(uid, "explorador")
    }

    // ── Video completado (con badges) ────────────────────────────────

    suspend fun cobrarVideoConBadges(uid: String, amount: Int = 50): Result<Unit> = runCatching {
        // Leer usuario y resetear diario si es un nuevo día
        var user = getUser(uid).getOrNull() ?: return@runCatching
        user = checkAndResetDaily(uid, user)

        // Verificar límite diario de videos
        val yaGanado = user.tokenVideosHoy
        if (yaGanado >= DAILY_LIMIT_VIDEOS) {
            // Límite alcanzado: solo sumamos el video al contador, sin acreditar MOVE
            userRef(uid).update("videosCompletadosTotales", FieldValue.increment(1L)).await()
            return@runCatching
        }

        userRef(uid).update(
            mapOf(
                "tokensActuales"           to FieldValue.increment(amount.toLong()),
                "tokenVideosHoy"           to FieldValue.increment(amount.toLong()),
                "videosCompletadosTotales" to FieldValue.increment(1L)
            )
        ).await()

        val limiteMensaje = if (yaGanado + amount >= DAILY_LIMIT_VIDEOS) " · límite diario alcanzado" else ""
        crearNotificacion(uid, Notificacion(
            uid       = uid,
            titulo    = "Video completado 🎬",
            cuerpo    = "+$amount MOVE acreditados$limiteMensaje",
            tipo      = "video",
            createdAt = System.currentTimeMillis()
        ))

        // Badges de videos (siempre se evalúan aunque no haya MOVE)
        val userActualizado = getUser(uid).getOrNull() ?: return@runCatching
        val total  = userActualizado.videosCompletadosTotales + 1
        val badges = userActualizado.badges
        if ("primer_video" !in badges)                       otorgarInsignia(uid, "primer_video")
        if (total >= 50 && "video_adicto" !in badges)        otorgarInsignia(uid, "video_adicto")
        else if (total >= 10 && "video_fan" !in badges)      otorgarInsignia(uid, "video_fan")
        val racha = userActualizado.diasConsecutivosVideos + 1
        if (racha >= 7 && "racha_7_videos" !in badges)       otorgarInsignia(uid, "racha_7_videos")
        else if (racha >= 5 && "racha_5_videos" !in badges)  otorgarInsignia(uid, "racha_5_videos")
    }

    // ── Holding con badges ───────────────────────────────────────────

    suspend fun createHoldingConBadges(uid: String, holding: Holding): Result<Unit> = runCatching {
        createHolding(uid, holding).getOrThrow()
        val user   = getUser(uid).getOrNull() ?: return@runCatching
        val badges = user.badges
        if ("primer_holding" !in badges) otorgarInsignia(uid, "primer_holding")
        val totalHolding = user.moveEnHolding + holding.moveAmount
        if (totalHolding >= 10_000 && "gran_inversor" !in badges) otorgarInsignia(uid, "gran_inversor")
    }

    // ── Blockchain ───────────────────────────────────────────────────

    private fun retirosRef(uid: String) = userRef(uid).collection("retirosBlockchain")

    /** Lee la configuración blockchain desde config/blockchain */
    suspend fun getBlockchainConfig(): Result<BlockchainConfig> = runCatching {
        val snap = db.collection("config").document("blockchain").get().await()
        if (!snap.exists()) return@runCatching BlockchainConfig()
        BlockchainConfig(
            enabled          = snap.getBoolean("enabled") ?: false,
            red              = snap.getString("red") ?: "BNB Smart Chain",
            redTicker        = snap.getString("redTicker") ?: "BNB",
            feePercent       = (snap.getDouble("feePercent") ?: 3.0).toFloat(),
            feeWalletAddress = snap.getString("feeWalletAddress") ?: "",
            minRetiro        = (snap.getLong("minRetiro") ?: 1_000L).toInt(),
            maxRetiro        = (snap.getLong("maxRetiro") ?: 100_000L).toInt(),
            tasaCambioInfo   = snap.getString("tasaCambioInfo") ?: "",
            metaUsuarios     = (snap.getLong("metaUsuarios") ?: 5_000L).toInt(),
            mensajeBloqueado = snap.getString("mensajeBloqueado") ?: BlockchainConfig().mensajeBloqueado
        )
    }

    /** Guarda la wallet address del usuario en su perfil */
    suspend fun saveWalletAddress(uid: String, address: String): Result<Unit> = runCatching {
        userRef(uid).update("walletAddress", address.trim()).await()
    }

    /** Crea una solicitud de retiro y descuenta MOVE del saldo */
    suspend fun solicitarRetiro(uid: String, retiro: RetiroBlockchain): Result<Unit> = runCatching {
        val ref   = retirosRef(uid).document()
        val final = retiro.copy(id = ref.id, uid = uid, createdAt = System.currentTimeMillis())
        val batch = db.batch()
        batch.set(ref, final)
        batch.update(userRef(uid), "tokensActuales", FieldValue.increment(-retiro.moveTotal.toLong()))
        batch.commit().await()
        crearNotificacion(uid, Notificacion(
            uid       = uid,
            titulo    = "Retiro blockchain solicitado 📤",
            cuerpo    = "${retiro.moveNeto} MOVE → ${retiro.walletDestino.take(12)}… · pendiente",
            tipo      = "blockchain",
            createdAt = System.currentTimeMillis()
        ))
    }

    /** Historial de retiros del usuario */
    suspend fun getRetiros(uid: String): Result<List<RetiroBlockchain>> = runCatching {
        retirosRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(RetiroBlockchain::class.java)
    }

    // ── Misiones diarias ─────────────────────────────────────────────

    /**
     * Reclama la recompensa de una misión diaria.
     * Valida que la misión esté completada y no haya sido reclamada antes.
     * @param misionId  ID de la misión ("caminar_2km", "videos_4", "racha_3")
     * @param recompensa MOVE a acreditar al usuario
     * @return Result<Unit> — falla si ya reclamada o no completada
     */
    suspend fun reclamarMision(uid: String, misionId: String, recompensa: Int): Result<Unit> = runCatching {
        var user = getUser(uid).getOrNull() ?: error("Usuario no encontrado")
        user = checkAndResetDaily(uid, user)

        if (misionId in user.misionesReclamadasHoy) error("Misión ya reclamada")

        userRef(uid).update(
            mapOf(
                "tokensActuales"        to FieldValue.increment(recompensa.toLong()),
                "misionesReclamadasHoy" to FieldValue.arrayUnion(misionId)
            )
        ).await()

        crearNotificacion(uid, Notificacion(
            uid       = uid,
            titulo    = "¡Misión completada! 🎯",
            cuerpo    = "+$recompensa MOVE acreditados",
            tipo      = "mision",
            createdAt = System.currentTimeMillis()
        ))
    }

    // ── Garantizar badge soy_move (usuarios existentes sin badges) ───

    suspend fun garantizarBadgeBienvenida(uid: String): Result<Unit> = runCatching {
        val user = getUser(uid).getOrNull() ?: return@runCatching
        if (user.badges.isEmpty()) {
            otorgarInsignia(uid, "soy_move").getOrThrow()
        }
    }

    // ── Sesiones activas en el mapa ──────────────────────────────────────

    private fun sesionesActivasRef() = db.collection("sesionesActivas")

    /** Escribe / actualiza la posición del usuario en el mapa en tiempo real */
    suspend fun upsertSesionActiva(uid: String, sesion: SesionActiva): Result<Unit> = runCatching {
        sesionesActivasRef().document(uid).set(sesion).await()
    }

    /** Elimina la sesión activa del usuario (cuando detiene MOVErme) */
    suspend fun deleteSesionActiva(uid: String): Result<Unit> = runCatching {
        sesionesActivasRef().document(uid).delete().await()
    }

    /**
     * Flow en tiempo real de todas las sesiones activas.
     * Filtra automáticamente sesiones "fantasma" con más de 5 minutos de antigüedad
     * (por si el servicio murió sin limpiar).
     */
    fun getSesionesActivasFlow(): kotlinx.coroutines.flow.Flow<List<SesionActiva>> =
        callbackFlow {
            val staleMs = 5 * 60 * 1000L  // 5 minutos
            val listener = sesionesActivasRef().addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val now      = System.currentTimeMillis()
                val sessions = snap?.toObjects(SesionActiva::class.java)
                    ?.filter { it.updatedAt > now - staleMs } ?: emptyList()
                trySend(sessions)
            }
            awaitClose { listener.remove() }
        }
}
