package com.flowly.move.data.model

data class StoreProduct(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val moveRequerido: Int = 0,
    val montoLabel: String = "",   // ej. "$500 ARS"
    val categoria: String = "",    // "cash" | "gift" | "promo"
    val activo: Boolean = true,
    val imagenUrl: String = ""     // URL de Firebase Storage (puede estar vacía)
)

data class StoreConfig(
    val umbralUsuarios: Int = 500,      // cuántos usuarios para abrir la tienda
    val productos: List<StoreProduct> = emptyList(),
    // URL base del link de referidos (se concatena el código del usuario al final)
    val referralBaseUrl: String = "https://flowly.app/r/",
    // URL de Mercado Pago del admin (configurable desde el panel)
    val mercadoPagoUrl: String = "",
    // URL de YouTube para mostrar como banner en Home
    val youtubeUrl: String = "",
    // Quiz del video — versión cambia cuando el admin actualiza las preguntas
    val videoQuizVersion: String = "",
    val videoQuiz: List<VideoQuestion> = emptyList()
)

/** Una pregunta del quiz del video con dos opciones. */
data class VideoQuestion(
    val pregunta: String = "",
    val opciones: List<String> = emptyList(),
    val correcta: Int = 0          // índice de la opción correcta (0 ó 1)
)

// Productos por defecto (se muestran si Firestore no tiene config aún)
val DEFAULT_STORE_PRODUCTS = listOf(
    StoreProduct(
        id            = "cash_500",
        nombre        = "Recompensa Nivel 1",
        descripcion   = "Beneficio exclusivo para miembros activos",
        moveRequerido = 5_000,
        montoLabel    = "Nivel 1",
        categoria     = "cash"
    ),
    StoreProduct(
        id            = "cash_1000",
        nombre        = "Recompensa Nivel 2",
        descripcion   = "Beneficio exclusivo para miembros activos",
        moveRequerido = 9_500,
        montoLabel    = "Nivel 2",
        categoria     = "cash"
    ),
    StoreProduct(
        id            = "cash_2000",
        nombre        = "Recompensa Nivel 3",
        descripcion   = "Beneficio exclusivo para miembros activos",
        moveRequerido = 18_000,
        montoLabel    = "Nivel 3",
        categoria     = "cash"
    ),
    StoreProduct(
        id            = "gift_cafe",
        nombre        = "Café gratis",
        descripcion   = "Voucher para café en locales adheridos",
        moveRequerido = 3_000,
        montoLabel    = "Voucher",
        categoria     = "gift"
    ),
    StoreProduct(
        id            = "gift_descuento",
        nombre        = "10% descuento",
        descripcion   = "En comercios adheridos a MOVE",
        moveRequerido = 1_500,
        montoLabel    = "Descuento",
        categoria     = "promo"
    )
)
