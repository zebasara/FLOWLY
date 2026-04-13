package com.flowly.move.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Límites de tokens por nivel. Índice 0 = Nivel 1, índice 9 = Nivel 10.
 * Esta es la fuente de verdad — ignorar el campo limiteTokens guardado en Firestore.
 */
val NIVEL_LIMITES = listOf(
    20_000,    // Nivel 1
    50_000,    // Nivel 2
    84_000,    // Nivel 3
    140_000,   // Nivel 4
    210_000,   // Nivel 5
    300_000,   // Nivel 6
    420_000,   // Nivel 7
    560_000,   // Nivel 8
    720_000,   // Nivel 9
    1_000_000  // Nivel 10
)

data class User(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val provincia: String = "",
    val ciudad: String = "",
    val aliasMercadoPago: String = "",
    // Sistema de niveles
    val nivel: Int = 1,
    val limiteTokens: Int = 2_000,
    val tokensActuales: Int = 0,
    // Progreso de nivel (doble condición)
    val diasConsecutivosVideos: Int = 0,
    val videosAcumuladosNivel: Int = 0,   // resetea al subir de nivel
    val saldoMinimoParaSubir: Int = 1_500, // 75% del limiteTokens actual
    // Actividad diaria
    val tokenMovimientoHoy: Int = 0,
    val tokenVideosHoy: Int = 0,
    val move30Dias: Int = 0,
    val lastTokenResetDate: String = "",   // "yyyy-MM-dd" — para reset diario automático
    // Distancia GPS
    val kmHoy: Float = 0f,
    val kmTotales: Float = 0f,
    // Videos
    val videosCompletadosTotales: Int = 0,
    val lastVideoDate: String = "",    // "yyyy-MM-dd" último día que completó un video
    // Holding
    val moveEnHolding: Int = 0,
    // Referidos
    val totalReferidos: Int = 0,
    val referidoPor: String = "",
    // Insignias
    val badges: List<String> = emptyList(),
    // Misiones diarias — se resetea junto con los contadores diarios
    val misionesReclamadasHoy: List<String> = emptyList(),
// Campeón semanal
    val campeonSemanalRacha: Int = 0,      // semanas consecutivas como campeón (0 = nunca)
    // Admin — campo en Firestore se llama "admin"
    @get:PropertyName("admin") @set:PropertyName("admin") var isAdmin: Boolean = false,
    // Quiz del video — guarda la versión del quiz que ya respondió
    val videoQuizAnsweredVersion: String = "",
    // Metadata
    val createdAt: Long = 0L,
    val profilePhotoUrl: String = "",
    // Blockchain — campo opcional, presente solo si el usuario activó retiro cripto
    val walletAddress: String = ""
)

/**
 * Iniciales del usuario — propiedad de extensión calculada en cliente.
 * NO se almacena en Firestore. Si Firestore tiene un campo "iniciales",
 * CustomClassMapper lo ignora porque no hay setter en la data class.
 */
val User.iniciales: String get() {
    val parts = nombre.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "??"
    }
}
