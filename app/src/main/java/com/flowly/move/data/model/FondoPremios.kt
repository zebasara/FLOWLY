package com.flowly.move.data.model

/**
 * Fondo de Premios Mensual — financiado por ingresos AdMob.
 * Se guarda en Firestore: config/fondoPremios
 *
 * El admin actualiza [montoTotal] cada mes desde el panel web
 * luego de revisar los ingresos de AdMob.
 * Al cierre del mes, se reparte entre el top 10 del ranking mensual.
 */
data class FondoPremios(
    val mes: String = "",            // "yyyy-MM" — mes vigente, ej. "2026-03"
    val montoDolares: Double = 0.0,  // monto en USD (ingresado por el admin desde AdMob)
    val porcentajeAdmob: Int = 35,   // % de AdMob destinado (informativo para la UI)
    val blueRateCache: Double = 0.0, // último tipo de cambio blue guardado por el admin — fallback si la API falla
    val updatedAt: Long = 0L
)

/**
 * Distribución fija por posición (índice 0 = 1er puesto … índice 9 = 10mo puesto).
 * Suma 100%.
 */
val DISTRIBUCION_FONDO = listOf(40, 20, 12, 4, 4, 4, 4, 4, 4, 4)
