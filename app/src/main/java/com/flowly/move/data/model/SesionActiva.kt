package com.flowly.move.data.model

data class SesionActiva(
    val uid:             String = "",
    val nombre:          String = "",
    val profilePhotoUrl: String = "",
    val lat:             Double = 0.0,
    val lng:             Double = 0.0,
    val updatedAt:       Long   = 0L
) {
    val iniciales: String get() {
        val parts = nombre.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "??"
        }
    }
}
