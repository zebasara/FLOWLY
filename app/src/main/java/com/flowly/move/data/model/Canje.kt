package com.flowly.move.data.model

data class Canje(
    val id: String = "",
    val uid: String = "",
    val montoLabel: String = "",      // ej: "$2.000 ARS"
    val moveDescontado: Int = 0,
    val aliasDestino: String = "",
    val estado: String = "pendiente", // pendiente | completado | enviado | cancelado
    val createdAt: Long = 0L
)
