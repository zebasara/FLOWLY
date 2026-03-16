package com.flowly.move.data.model

/**
 * Configuración de la sección blockchain.
 * Se guarda en Firestore: config/blockchain
 * El admin la controla desde el panel web.
 */
data class BlockchainConfig(
    // ── Control de activación ────────────────────────────────
    val enabled: Boolean = false,

    // ── Red blockchain ───────────────────────────────────────
    val red: String = "BNB Smart Chain",       // nombre visible al usuario
    val redTicker: String = "BNB",             // símbolo del gas token

    // ── Comisión ─────────────────────────────────────────────
    val feePercent: Float = 3f,                // % de MOVE que se cobra al retirar
    val feeWalletAddress: String = "",         // wallet del admin que recibe la comisión

    // ── Límites ──────────────────────────────────────────────
    val minRetiro: Int = 1_000,                // mínimo MOVE para solicitar retiro
    val maxRetiro: Int = 100_000,              // máximo MOVE por solicitud

    // ── Info tasa de cambio (texto libre, solo informativo) ──
    val tasaCambioInfo: String = "",           // ej. "1.000 MOVE ≈ 1 USDT"

    // ── Pantalla bloqueada ───────────────────────────────────
    val metaUsuarios: Int = 5_000,             // meta de usuarios para el progreso
    val mensajeBloqueado: String = "Llevamos el token MOVE a blockchain. " +
        "Cuando lleguemos a nuestra meta podés retirar tus MOVE a tu wallet y tradearlos."
)
