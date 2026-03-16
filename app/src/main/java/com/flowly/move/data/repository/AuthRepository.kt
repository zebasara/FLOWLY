package com.flowly.move.data.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.flowly.move.BuildConfig
import com.flowly.move.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    // ── Email / Password ────────────────────────────────────────

    suspend fun loginWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("Usuario no encontrado")
    }

    suspend fun registerWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.uid ?: error("Error al crear usuario")
    }

    // ── Google Sign-In (Credential Manager) ─────────────────────

    suspend fun signInWithGoogle(activity: Activity): Result<Pair<String, Boolean>> = runCatching {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
            Pair(authResult.user?.uid ?: error("Error en autenticación"), isNewUser)
        } else {
            error("Tipo de credencial no soportado")
        }
    }

    // ── Firestore ───────────────────────────────────────────────

    suspend fun saveUser(user: User): Result<Unit> = runCatching {
        db.collection("usuarios").document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): Result<User?> = runCatching {
        db.collection("usuarios").document(uid).get().await().toObject(User::class.java)
    }

    suspend fun updateField(uid: String, field: String, value: Any): Result<Unit> = runCatching {
        db.collection("usuarios").document(uid).update(field, value).await()
    }

    suspend fun isProfileComplete(uid: String): Boolean = runCatching {
        val user = db.collection("usuarios").document(uid).get().await()
            .toObject(User::class.java)
        user != null && user.nombre.isNotBlank() && user.telefono.isNotBlank()
    }.getOrDefault(false)

    // ── Sign Out ────────────────────────────────────────────────

    fun signOut() = auth.signOut()
}
