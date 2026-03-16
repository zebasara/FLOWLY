package com.flowly.move.data.model

data class Holding(
    val id: String = "",
    val uid: String = "",
    val moveAmount: Int = 0,
    val meses: Int = 3,
    val interesMove: Int = 0,
    val fechaInicio: Long = 0L,
    val fechaFin: Long = 0L,
    val estado: String = "activo"     // activo | completado | cancelado
)
