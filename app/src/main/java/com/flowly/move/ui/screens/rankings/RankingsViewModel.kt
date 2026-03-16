package com.flowly.move.ui.screens.rankings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.data.model.User
import com.flowly.move.data.repository.FlowlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RankingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FlowlyRepository(app)
    private val prefs = UserPreferences(app)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _rankings    = MutableStateFlow<List<User>>(emptyList())
    val rankings: StateFlow<List<User>> = _rankings.asStateFlow()

    private val _isLoading   = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var uid      = ""
    private var ciudad   = ""
    private var provincia = ""

    init {
        viewModelScope.launch {
            uid = prefs.userId.first()
            repo.getUser(uid).onSuccess {
                _currentUser.value = it
                ciudad    = it?.ciudad   ?: ""
                provincia = it?.provincia ?: ""
            }
            loadRankings("all")
        }
    }

    fun loadRankings(scope: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getRankings(scope, ciudad, provincia).onSuccess {
                _rankings.value = it
            }
            _isLoading.value = false
        }
    }
}
