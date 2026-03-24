package com.flowly.move.data.model

data class Notificacion(
    val id: String = "",
    val uid: String = "",
    val titulo: String = "",
    val cuerpo: String = "",
    val tipo: String = "info",  // info | logro | pago | video | referido | movimiento | mision | nivel | campeon
    val leida: Boolean = false,
    val createdAt: Long = 0L
)
