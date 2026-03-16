package com.flowly.move.data.repository

import android.content.Context
import android.net.Uri
import com.flowly.move.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FlowlyRepository(private val context: Context) {

    private val db      = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
        val umbral    = (snap.getLong("umbralUsuarios") ?: 500L).toInt()
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
                activo        = m["activo"] as? Boolean ?: true
            )
        }
        StoreConfig(
            umbralUsuarios = umbral,
            productos      = if (productos.isEmpty()) DEFAULT_STORE_PRODUCTS else productos
        )
    }

    suspend fun getUserCount(): Result<Long> = runCatching {
        db.collection("usuarios").count().get(
            com.google.firebase.firestore.AggregateSource.SERVER
        ).await().count
    }
}
