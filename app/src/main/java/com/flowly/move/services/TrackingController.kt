package com.flowly.move.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackingStats(
    val isTracking:             Boolean = false,
    val distanceMeters:         Float   = 0f,
    val durationSeconds:        Long    = 0L,
    val speedKmh:               Float   = 0f,
    val steps:                  Int     = 0,
    val lastLat:                Double  = 0.0,
    val lastLng:                Double  = 0.0,
    /** Metros ya acreditados progresivamente en Firestore durante esta sesión */
    val distanceCreditedMeters: Float   = 0f
)

/**
 * Singleton que sirve como canal de comunicación entre
 * TrackingForegroundService y MapViewModel.
 */
object TrackingController {
    private val _stats = MutableStateFlow(TrackingStats())
    val stats: StateFlow<TrackingStats> = _stats.asStateFlow()

    /** Momento en milisegundos en que arrancó el tracking (0 = sin sesión activa). */
    var startTimeMs: Long = 0L
        private set

    fun update(stats: TrackingStats) { _stats.value = stats }

    fun markStart() { startTimeMs = System.currentTimeMillis() }

    fun reset() {
        _stats.value = TrackingStats()
        startTimeMs  = 0L
    }
}
