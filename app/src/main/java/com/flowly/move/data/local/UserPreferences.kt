package com.flowly.move.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flowly_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val IS_LOGGED_IN          = booleanPreferencesKey("is_logged_in")
        val USER_ID               = stringPreferencesKey("user_id")
        val USER_EMAIL            = stringPreferencesKey("user_email")
        val USER_NAME             = stringPreferencesKey("user_name")
        val PROFILE_COMPLETE      = booleanPreferencesKey("profile_complete")
        val ONBOARDING_DONE       = booleanPreferencesKey("onboarding_done")
        val WELCOME_DIALOG_SHOWN  = booleanPreferencesKey("welcome_dialog_shown")
        val SHOWN_BADGES          = stringSetPreferencesKey("shown_badges")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[IS_LOGGED_IN] ?: false }

    val userId: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[USER_ID] ?: "" }

    val userEmail: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[USER_EMAIL] ?: "" }

    val userName: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[USER_NAME] ?: "" }

    val isProfileComplete: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PROFILE_COMPLETE] ?: false }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ONBOARDING_DONE] ?: false }

    val welcomeDialogShown: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[WELCOME_DIALOG_SHOWN] ?: false }

    val shownBadges: Flow<Set<String>> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SHOWN_BADGES] ?: emptySet() }

    suspend fun setWelcomeDialogShown() {
        context.dataStore.edit { it[WELCOME_DIALOG_SHOWN] = true }
    }

    suspend fun markBadgeShown(badgeId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SHOWN_BADGES] ?: emptySet()
            prefs[SHOWN_BADGES] = current + badgeId
        }
    }

    suspend fun setLoggedIn(uid: String, email: String, name: String) {
        context.dataStore.edit {
            it[IS_LOGGED_IN]  = true
            it[USER_ID]       = uid
            it[USER_EMAIL]    = email
            it[USER_NAME]     = name
        }
    }

    suspend fun setProfileComplete(name: String = "") {
        context.dataStore.edit {
            it[PROFILE_COMPLETE] = true
            if (name.isNotBlank()) it[USER_NAME] = name
        }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
