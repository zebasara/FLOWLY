package com.flowly.move.data.model

/**
 * Solicitud de retiro blockchain de un usuario.
 * Subcolección: usuarios/{uid}/retirosBlockchain/{id}
 *
 * Flujo:
 *  1. Usuario crea la solicitud → estado = "pendiente"
 *  2. Admin procesa manualmente, envía la crypto a walletDestino
 *  3. Admin marca estado = "procesado" y llena txHash
 */
data class RetiroBlockchain(
    val id: String = "",
    val uid: String = "",

    // ── Wallet destino ───────────────────────────────────────
    val walletDestino: String = "",
    val red: String = "",

    // ── Montos en MOVE ───────────────────────────────────────
    val moveTotal: Int = 0,        // MOVE descontado del saldo del usuario
    val moveFee: Int = 0,          // MOVE de comisión (va al admin)
    val moveNeto: Int = 0,         // MOVE que se envían a la wallet (= moveTotal - moveFee)
    val feePercent: Float = 3f,    // porcentaje aplicado (snapshot al momento del retiro)

    // ── Estado ───────────────────────────────────────────────
    val estado: String = "pendiente",  // pendiente | procesado | rechazado
    val txHash: String = "",           // hash de la tx blockchain (lo llena el admin)
    val notaAdmin: String = "",        // nota del admin si rechaza

    // ── Metadata ─────────────────────────────────────────────
    val createdAt: Long = 0L
)
