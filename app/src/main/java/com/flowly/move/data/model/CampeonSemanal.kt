package com.flowly.move.data.model

/**
 * Campeón Semanal de Argentina.
 * Se guarda en Firestore: config/campeon
 *
 * - [semana]      : "yyyy-Www"  (ej. "2026-W12") — evita asignaciones dobles
 * - [racha]       : semanas consecutivas como campeón (mínimo 1)
 * - [assignedAt]  : timestamp del lunes en que fue asignado
 */
data class CampeonSemanal(
    val uid: String = "",
    val nombre: String = "",
    val provincia: String = "",
    val ciudad: String = "",
    val tokensActuales: Int = 0,
    val photoUrl: String = "",
    val racha: Int = 1,
    val semana: String = "",      // "yyyy-Www"
    val assignedAt: Long = 0L
)
