package com.flowly.move.data.model

/**
 * Una opción de canje configurada desde el admin de Firestore.
 *
 * @param id      Identificador único ("2000_ars", "5000_ars", …)
 * @param label   Texto a mostrar en la app ("$2.000 ARS")
 * @param move    MOVE requeridos para este canje
 * @param activo  false → la opción no aparece en la app (sin eliminarla del doc)
 */
data class CanjeOferta(
    val id: String = "",
    val label: String = "",
    val move: Int = 0,
    val activo: Boolean = true
)

/**
 * Configuración completa de canjes leída desde config/canjes en Firestore.
 *
 * @param opciones      Lista de ofertas disponibles (ordenadas como lleguen de Firestore)
 * @param notaMensaje   Texto informativo que aparece debajo de las opciones
 * @param nivelMinimo   Nivel mínimo que debe tener el usuario para poder canjear (1 = todos)
 */
data class CanjesConfig(
    val opciones: List<CanjeOferta> = DEFAULT_CANJE_OFERTAS,
    val notaMensaje: String = "Una recompensa por mes · disponible dentro del período de validación",
    val nivelMinimo: Int = 1
)

/**
 * Fallback: se usa si Firestore no tiene el documento config/canjes todavía.
 * Refleja las tasas actuales hardcodeadas. Editá estas en Firestore para ajustar.
 */
val DEFAULT_CANJE_OFERTAS = listOf(
    CanjeOferta(id = "2000_ars",  label = "Recompensa Nivel 1",  move = 33_600,  activo = true),
    CanjeOferta(id = "5000_ars",  label = "Recompensa Nivel 2",  move = 84_000,  activo = true),
    CanjeOferta(id = "10000_ars", label = "Recompensa Nivel 3", move = 168_000, activo = true),
    CanjeOferta(id = "20000_ars", label = "Recompensa Nivel 4", move = 336_000, activo = true)
)
