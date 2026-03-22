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
        const val VIDEO_REWARD_AMOUNT    = 50    // MOVE acreditados por cada video completado
        // Máximo teórico: 700 MOVE/día → saldo mínimo nivel 1 (15.000) en ~21 días
    }

    /** Devuelve "yyyy-MM-dd" del día de hoy en zona local */
    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /** Devuelve "yyyy-MM-dd" de ayer en zona local */
    private fun yesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    /** Devuelve "yyyy-MM-dd" de hace N días en zona local */
    private fun daysAgo(n: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    /**
     * Verifica si los contadores diarios del usuario necesitan resetearse.
     * Si la fecha guardada ≠ hoy, resetea tokenMovimientoHoy, tokenVideosHoy, kmHoy
     * y misionesReclamadasHoy. También resetea move30Dias si han pasado 30 días.
     * Retorna el usuario actualizado (con los contadores ya reseteados si corresponde).
     */
    private suspend fun checkAndResetDaily(uid: String, user: com.flowly.move.data.model.User): com.flowly.move.data.model.User {
        val todayStr = today()
        if (user.lastTokenResetDate == todayStr) return user   // ya está en el día de hoy

        // Determinar si también hay que resetear el acumulado de 30 días.
        // Si lastTokenResetDate es anterior a hace 30 días → reset move30Dias.
        val cutoff30 = daysAgo(30)
        val reset30 = user.lastTokenResetDate.isBlank() || user.lastTokenResetDate <= cutoff30

        // Es un día nuevo → resetear contadores diarios
        val updates = mutableMapOf<String, Any>(
            "tokenMovimientoHoy"    to 0,
            "tokenVideosHoy"        to 0,
            "kmHoy"                 to 0f,
            "lastTokenResetDate"    to todayStr,
            "misionesReclamadasHoy" to emptyList<String>()
        )
        if (reset30) updates["move30Dias"] = 0

        userRef(uid).update(updates).await()
        return user.copy(
            tokenMovimientoHoy    = 0,
            tokenVideosHoy        = 0,
            kmHoy                 = 0f,
            lastTokenResetDate    = todayStr,
            misionesReclamadasHoy = emptyList(),
            move30Dias            = if (reset30) 0 else user.move30Dias
        )
    }

    /**
     * Verifica y ejecuta el reset diario si el día cambió.
     * Llamar al abrir la pantalla de misiones o al iniciar la app.
     */
    suspend fun ensureDailyReset(uid: String): Result<User?> = runCatching {
        val user = getUser(uid).getOrNull() ?: return@runCatching null
        checkAndResetDaily(uid, user)
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
        // Insignia de primer canje
        val user = getUser(uid).getOrNull()
        if (user != null && "primer_canje" !in user.badges) {
            otorgarInsignia(uid, "primer_canje")
        }
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

    /**
     * Consulta el ranking de usuarios según el scope indicado.
     *
     * - "all"       → top 50 de Argentina (sin filtro, ordenado por tokensActuales)
     * - "provincia" → filtra por provincia directamente en Firestore (requiere índice
     *                 compuesto: provincia ASC + tokensActuales DESC en la consola Firebase)
     * - "ciudad"    → filtra por ciudad directamente en Firestore (requiere índice
     *                 compuesto: ciudad ASC + tokensActuales DESC en la consola Firebase)
     *
     * Nota: los índices compuestos se crean automáticamente la primera vez que se
     * ejecuta la consulta — Firestore muestra el link en Logcat para crearlos.
     */
    suspend fun getRankings(
        scope: String,
        ciudad: String = "",
        provincia: String = ""
    ): Result<List<User>> = runCatching {
        when (scope) {
            "ciudad" -> {
                if (ciudad.isBlank()) emptyList()
                else db.collection("usuarios")
                    .whereEqualTo("ciudad", ciudad)
                    .orderBy("tokensActuales", Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                    .toObjects(User::class.java)
            }
            "provincia" -> {
                if (provincia.isBlank()) emptyList()
                else db.collection("usuarios")
                    .whereEqualTo("provincia", provincia)
                    .orderBy("tokensActuales", Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                    .toObjects(User::class.java)
            }
            else -> {
                db.collection("usuarios")
                    .orderBy("tokensActuales", Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                    .toObjects(User::class.java)
            }
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

    /**
     * Acredita +200 MOVE al referidor, incrementa su contador, otorga insignias y crea notificación.
     */
    suspend fun registrarReferido(uidReferidor: String): Result<Unit> = runCatching {
        // Leer estado antes de incrementar para calcular insignias
        val referidor = getUser(uidReferidor).getOrNull()

        userRef(uidReferidor).update(
            mapOf(
                "totalReferidos" to FieldValue.increment(1),
                "tokensActuales" to FieldValue.increment(200L)
            )
        ).await()

        // Notificación al referidor
        crearNotificacion(uidReferidor, Notificacion(
            uid       = uidReferidor,
            titulo    = "¡Nuevo referido! 👥",
            cuerpo    = "+200 MOVE acreditados por tu invitación",
            tipo      = "referido",
            createdAt = System.currentTimeMillis()
        ))

        // Insignias de referidos (nuevo total = viejo + 1)
        if (referidor != null) {
            val newTotal = referidor.totalReferidos + 1
            val badges   = referidor.badges
            when {
                newTotal >= 10 && "10_referidos"    !in badges -> otorgarInsignia(uidReferidor, "10_referidos")
                newTotal >= 5  && "5_referidos"     !in badges -> otorgarInsignia(uidReferidor, "5_referidos")
                newTotal >= 1  && "primer_referido" !in badges -> otorgarInsignia(uidReferidor, "primer_referido")
            }
        }
    }

    /**
     * Otorga +100 MOVE al USUARIO NUEVO por haber usado un código de referido válido.
     */
    suspend fun otorgarBonoReferido(uid: String): Result<Unit> = runCatching {
        userRef(uid).update("tokensActuales", FieldValue.increment(100L)).await()
        crearNotificacion(uid, Notificacion(
            uid       = uid,
            titulo    = "¡Código de referido válido! 🎉",
            cuerpo    = "+100 MOVE extra de bienvenida",
            tipo      = "referido",
            createdAt = System.currentTimeMillis()
        ))
    }

    /**
     * Busca el UID completo a partir de un código de referido (primeros 8 chars del UID).
     * Acepta tanto el código corto como la URL completa (extrae los últimos 8 chars tras "/").
     */
    suspend fun findUidByReferralCode(code: String): Result<String?> = runCatching {
        val cleanCode = code.trim().substringAfterLast("/").trim()
        if (cleanCode.isBlank()) return@runCatching null
        val snap = db.collection("usuarios")
            .whereGreaterThanOrEqualTo("uid", cleanCode)
            .whereLessThan("uid", cleanCode + "\uF7FF")
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

    // ── Canjes (config dinámica desde admin) ─────────────────────

    /**
     * Lee la configuración de canjes desde config/canjes.
     * Si el documento no existe todavía, devuelve los valores por defecto.
     */
    suspend fun getCanjesConfig(): Result<CanjesConfig> = runCatching {
        val snap = db.collection("config").document("canjes").get().await()
        if (!snap.exists()) return@runCatching CanjesConfig()

        val notaMensaje = snap.getString("notaMensaje")
            ?: "Un canje por mes · procesado en menos de 48hs hábiles"
        val nivelMinimo = (snap.getLong("nivelMinimo") ?: 1L).toInt()

        @Suppress("UNCHECKED_CAST")
        val rawOpciones = snap.get("opciones") as? List<Map<String, Any>> ?: emptyList()
        val opciones = rawOpciones.map { m ->
            CanjeOferta(
                id     = m["id"]     as? String  ?: "",
                label  = m["label"]  as? String  ?: "",
                move   = (m["move"]  as? Long)?.toInt() ?: 0,
                activo = m["activo"] as? Boolean ?: true
            )
        }

        CanjesConfig(
            opciones    = if (opciones.isEmpty()) DEFAULT_CANJE_OFERTAS else opciones,
            notaMensaje = notaMensaje,
            nivelMinimo = nivelMinimo
        )
    }

    /** Guarda toda la configuración de canjes en config/canjes (sobreescribe el doc). */
    suspend fun saveCanjesConfig(config: CanjesConfig): Result<Unit> = runCatching {
        val opciones = config.opciones.map { o ->
            mapOf(
                "id"     to o.id,
                "label"  to o.label,
                "move"   to o.move.toLong(),
                "activo" to o.activo
            )
        }
        db.collection("config").document("canjes").set(
            mapOf(
                "opciones"    to opciones,
                "notaMensaje" to config.notaMensaje,
                "nivelMinimo" to config.nivelMinimo.toLong()
            )
        ).await()
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
                "tokensActuales"     to FieldValue.increment(moveEarned.toLong()),
                "move30Dias"         to FieldValue.increment(moveEarned.toLong())
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
        // kmTotales ya fue incrementado por el update anterior — usarlo directo
        val userActualizado = getUser(uid).getOrNull() ?: return@runCatching
        val kmNuevos = userActualizado.kmTotales
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

        val todayStr = today()
        val yaGanado = user.tokenVideosHoy

        // ── Calcular racha de días consecutivos ────────────────────────────
        // Solo se actualiza al primer video del día (cuando yaGanado == 0).
        // Si el último video fue ayer → racha + 1.
        // Si hubo días sin ver videos → racha reinicia a 1.
        val newStreak: Int = if (yaGanado == 0) {
            if (user.lastVideoDate == yesterday()) user.diasConsecutivosVideos + 1
            else 1
        } else {
            user.diasConsecutivosVideos  // mismo día: no cambiar
        }

        if (yaGanado >= DAILY_LIMIT_VIDEOS) {
            // Límite de MOVE alcanzado: solo contamos el video y actualizamos racha
            userRef(uid).update(mapOf(
                "videosCompletadosTotales" to FieldValue.increment(1L),
                "diasConsecutivosVideos"   to newStreak,
                "lastVideoDate"            to todayStr
            )).await()
            return@runCatching
        }

        val limiteMensaje = if (yaGanado + amount >= DAILY_LIMIT_VIDEOS) " · límite diario alcanzado" else ""

        userRef(uid).update(
            mapOf(
                "tokensActuales"           to FieldValue.increment(amount.toLong()),
                "tokenVideosHoy"           to FieldValue.increment(amount.toLong()),
                "videosCompletadosTotales" to FieldValue.increment(1L),
                "diasConsecutivosVideos"   to newStreak,
                "lastVideoDate"            to todayStr
            )
        ).await()

        crearNotificacion(uid, Notificacion(
            uid       = uid,
            titulo    = "Video completado 🎬",
            cuerpo    = "+$amount MOVE acreditados$limiteMensaje",
            tipo      = "video",
            createdAt = System.currentTimeMillis()
        ))

        // Badges de videos
        // videosCompletadosTotales ya fue incrementado — usarlo directo
        val userActualizado = getUser(uid).getOrNull() ?: return@runCatching
        val total  = userActualizado.videosCompletadosTotales
        val badges = userActualizado.badges
        if ("primer_video" !in badges)                              otorgarInsignia(uid, "primer_video")
        if (total >= 50 && "video_adicto" !in badges)               otorgarInsignia(uid, "video_adicto")
        else if (total >= 10 && "video_fan" !in badges)             otorgarInsignia(uid, "video_fan")
        // Racha badges usando el valor calculado (no re-leer para evitar delay)
        if (newStreak >= 7 && "racha_7_videos" !in badges)          otorgarInsignia(uid, "racha_7_videos")
        else if (newStreak >= 5 && "racha_5_videos" !in badges)     otorgarInsignia(uid, "racha_5_videos")

        // Verificar si el usuario cumple condiciones para subir de nivel
        verificarYSubirNivel(uid)
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

    // ── Subida de nivel automática ───────────────────────────────────────

    /**
     * Verifica si el usuario cumple ambas condiciones para subir de nivel:
     *   1. diasConsecutivosVideos >= 5
     *   2. tokensActuales >= 75% del límite del nivel actual
     *
     * Si se cumplen, incrementa el nivel, actualiza limiteTokens, resetea
     * diasConsecutivosVideos, crea notificación y otorga badge "nivel_5" si aplica.
     *
     * @return true si se subió de nivel
     */
    suspend fun verificarYSubirNivel(uid: String): Boolean = runCatching {
        val user        = getUser(uid).getOrNull() ?: return@runCatching false
        val nivelActual = user.nivel
        if (nivelActual >= 10) return@runCatching false

        val limiteActual = NIVEL_LIMITES.getOrElse(nivelActual - 1) { return@runCatching false }
        val saldoMinimo  = limiteActual * 3 / 4   // 75%

        val videosOk = user.diasConsecutivosVideos >= 5
        val saldoOk  = user.tokensActuales >= saldoMinimo
        if (!videosOk || !saldoOk) return@runCatching false

        val nuevoNivel  = nivelActual + 1
        val nuevoLimite = NIVEL_LIMITES[nuevoNivel - 1]

        userRef(uid).update(mapOf(
            "nivel"                  to nuevoNivel,
            "limiteTokens"           to nuevoLimite,
            "diasConsecutivosVideos" to 0,
            "videosAcumuladosNivel"  to 0
        )).await()

        crearNotificacion(uid, Notificacion(
            uid       = uid,
            titulo    = "¡Subiste al Nivel $nuevoNivel! 🚀",
            cuerpo    = "Nuevo límite: %,d MOVE. ¡Seguí moviéndote!".format(nuevoLimite),
            tipo      = "nivel",
            createdAt = System.currentTimeMillis()
        ))

        // Badge especial de Nivel 5
        if (nuevoNivel == 5 && "nivel_5" !in user.badges) {
            otorgarInsignia(uid, "nivel_5")
        }

        true
    }.getOrDefault(false)

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

    // ── Campeón Semanal ──────────────────────────────────────────────────

    private fun campeonRef() = db.collection("config").document("campeon")

    /** Devuelve el campeón semanal actual guardado en Firestore (null si no existe). */
    suspend fun getCampeonSemanal(): Result<CampeonSemanal?> = runCatching {
        val snap = campeonRef().get().await()
        if (!snap.exists()) return@runCatching null
        CampeonSemanal(
            uid           = snap.getString("uid")           ?: "",
            nombre        = snap.getString("nombre")        ?: "",
            provincia     = snap.getString("provincia")     ?: "",
            ciudad        = snap.getString("ciudad")        ?: "",
            tokensActuales= (snap.getLong("tokensActuales") ?: 0L).toInt(),
            photoUrl      = snap.getString("photoUrl")      ?: "",
            racha         = (snap.getLong("racha")          ?: 1L).toInt(),
            semana        = snap.getString("semana")        ?: "",
            assignedAt    = snap.getLong("assignedAt")      ?: 0L
        )
    }

    /**
     * Determina y asigna al Campeón Semanal de Argentina.
     *
     * Se ejecuta solo si:
     *   1. Es lunes en el dispositivo
     *   2. La hora local es >= 15:00
     *   3. El campo "semana" en config/campeon NO coincide con la semana ISO actual
     *
     * El primer usuario que abra la app cada lunes después de las 15:00 activa
     * la asignación. El campo "semana" se escribe al inicio para evitar
     * ejecuciones simultáneas (optimistic lock).
     *
     * Otorga +1.000 MOVE al campeón y le envía una notificación.
     *
     * @return true si se asignó un nuevo campeón, false en cualquier otro caso.
     */
    suspend fun checkAndAssignCampeon(): Result<Boolean> = runCatching {
        val cal = Calendar.getInstance()

        // Calcular el lunes 15:00 de esta semana (momento de asignación)
        val assignmentDeadline = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // No ejecutar si todavía no llegó el lunes 15:00 de esta semana
        if (cal.before(assignmentDeadline)) return@runCatching false

        // Calcular la semana ISO actual, ej. "2026-W12"
        val weekYear = SimpleDateFormat("YYYY-'W'ww", Locale.ROOT).format(cal.time)

        val ref = campeonRef()

        // Leer el estado actual ANTES de escribir (optimistic check)
        val snapInicial = ref.get().await()
        val semanaGuardada = snapInicial.getString("semana") ?: ""
        if (semanaGuardada == weekYear) return@runCatching false   // ya asignado esta semana

        // Marcar semana como "en proceso" para reducir duplicaciones
        // (no es 100% atómico, pero el peor caso es 2 asignaciones en el mismo lunes)
        ref.set(mapOf("semana" to weekYear, "assignedAt" to System.currentTimeMillis()),
            com.google.firebase.firestore.SetOptions.merge()).await()

        // Buscar el usuario con más MOVE en Argentina
        val topUser = db.collection("usuarios")
            .orderBy("tokensActuales", Query.Direction.DESCENDING)
            .limit(1)
            .get().await()
            .toObjects(User::class.java)
            .firstOrNull() ?: return@runCatching false

        // Calcular racha: si el mismo UID ganó la semana anterior, incrementar
        val rachaAnterior = snapInicial.getLong("racha")?.toInt() ?: 0
        val uidAnterior   = snapInicial.getString("uid") ?: ""
        val nuevaRacha    = if (topUser.uid == uidAnterior) rachaAnterior + 1 else 1

        // Guardar campeón completo en config/campeon
        val campeon = CampeonSemanal(
            uid            = topUser.uid,
            nombre         = topUser.nombre,
            provincia      = topUser.provincia,
            ciudad         = topUser.ciudad,
            tokensActuales = topUser.tokensActuales,
            photoUrl       = topUser.profilePhotoUrl,
            racha          = nuevaRacha,
            semana         = weekYear,
            assignedAt     = System.currentTimeMillis()
        )
        ref.set(campeon).await()

        // Actualizar racha en el perfil del usuario
        userRef(topUser.uid).update("campeonSemanalRacha", nuevaRacha).await()

        // Acreditar +1.000 MOVE
        userRef(topUser.uid).update(
            "tokensActuales", FieldValue.increment(1_000L)
        ).await()

        // Notificación al campeón
        val rachaMsg = if (nuevaRacha > 1) " Racha: ×$nuevaRacha semanas 🔥" else ""
        crearNotificacion(topUser.uid, Notificacion(
            uid       = topUser.uid,
            titulo    = "¡Sos el Campeón Semanal! 🏆",
            cuerpo    = "+1.000 MOVE acreditados por liderar Argentina.$rachaMsg",
            tipo      = "campeon",
            createdAt = System.currentTimeMillis()
        ))

        true
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
