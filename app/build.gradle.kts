import java.util.Properties

// Carga secrets.properties para BuildConfig fields
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) load(secretsFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.flowly.move"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flowly.move"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Credenciales inyectadas desde secrets.properties
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID",
            "\"${secrets.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        // IDs de AdMob — si están vacíos en secrets.properties se usan los IDs de prueba oficiales
        val admobAppId = secrets.getProperty("ADMOB_APP_ID", "")
            .takeIf { it.isNotBlank() } ?: "ca-app-pub-3940256099942544~3347511713"
        val admobRewardedId = secrets.getProperty("ADMOB_REWARDED_AD_UNIT_ID", "")
            .takeIf { it.isNotBlank() } ?: "ca-app-pub-3940256099942544/5224354917"
        val admobBannerId = secrets.getProperty("ADMOB_BANNER_AD_UNIT_ID", "")
            .takeIf { it.isNotBlank() } ?: "ca-app-pub-3940256099942544/6300978111"

        buildConfigField("String", "ADMOB_APP_ID",            "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$admobRewardedId\"")
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID",   "\"$admobBannerId\"")
        buildConfigField("String", "MERCADOPAGO_PUBLIC_KEY",
            "\"${secrets.getProperty("MERCADOPAGO_PUBLIC_KEY", "")}\"")
        buildConfigField("String", "BASE_URL",
            "\"${secrets.getProperty("BASE_URL", "https://api.flowly.com/v1/")}\"")
        buildConfigField("String", "BNB_RPC_URL",
            "\"${secrets.getProperty("BNB_RPC_URL", "https://bsc-dataseed.binance.org/")}\"")
        buildConfigField("String", "FLOWLY_CONTRACT_ADDRESS",
            "\"${secrets.getProperty("FLOWLY_CONTRACT_ADDRESS", "0x0000000000000000000000000000000000000000")}\"")

        // admobAppId está hardcodeado directamente en AndroidManifest.xml
        // (evita crashes por placeholder vacío durante desarrollo)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = secrets.getProperty("KEYSTORE_PATH", "")
            val keystorePass = secrets.getProperty("KEYSTORE_PASSWORD", "")
            val keyAliasVal  = secrets.getProperty("KEY_ALIAS", "")
            val keyPass      = secrets.getProperty("KEY_PASSWORD", "")
            if (keystorePath.isNotBlank() && file(keystorePath).exists()) {
                storeFile     = file(keystorePath)
                storePassword = keystorePass
                keyAlias      = keyAliasVal
                keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig   = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.compose.activity)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // Navigation
    implementation(libs.nav.compose)

    // Lifecycle / ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // Google Credential Manager (Sign-In with Google)
    implementation(libs.credential.manager)
    implementation(libs.credential.manager.play)
    implementation(libs.google.id)

    // DataStore
    implementation(libs.datastore.prefs)

    // OSMDroid (mapa gratuito)
    implementation(libs.osmdroid)

    // Coil (carga de imágenes / fotos de perfil)
    implementation(libs.coil.compose)

    // AdMob
    implementation(libs.play.services.ads)

    // Material Icons Extended (SystemUpdate, Download, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase Storage (foto de perfil)
    implementation(libs.firebase.storage)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
