# ── Stack traces legibles ────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Firebase Auth + Firestore ────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepnames class com.google.firebase.firestore.** { *; }
# Mantener todos los data classes mapeados desde Firestore
-keep class com.flowly.move.data.model.** { *; }
-keepclassmembers class com.flowly.move.data.model.** { *; }

# ── Kotlin / Coroutines ──────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── OSMDroid ─────────────────────────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ── AdMob / Google Mobile Ads ────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }

# ── Credential Manager / Google Sign-In ─────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }

# ── Compose ──────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Web3j / BNB (si se usa reflexión) ───────────────────────────────────────
-dontwarn org.web3j.**
-keep class org.web3j.** { *; }

# ── Enums ────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
