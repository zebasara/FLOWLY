package com.flowly.move.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackingStats(
    val isTracking: Boolean = false,
    val distanceMeters: Float = 0f,
    val durationSeconds: Long = 0L,
    val speedKmh: Float = 0f,
    val steps: Int = 0
)

/**
 * Singleton que sirve como canal de comunicación entre
 * TrackingForegroundService y MapViewModel.
 */
object TrackingController {
    private val _stats = MutableStateFlow(TrackingStats())
    val stats: StateFlow<TrackingStats> = _stats.asStateFlow()

    fun update(stats: TrackingStats) { _stats.value = stats }

    fun reset() { _stats.value = TrackingStats() }
}
