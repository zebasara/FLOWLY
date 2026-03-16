package com.flowly.move.ui.screens.map

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.services.TrackingController
import com.flowly.move.services.TrackingForegroundService
import com.flowly.move.services.TrackingStats
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MapViewModel(app: Application) : AndroidViewModel(app) {

    val stats: StateFlow<TrackingStats> = TrackingController.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackingController.stats.value)

    fun startTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
