import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Firma opzionale. Se il keystore c'è davvero, TUTTE le build (anche quelle di
// debug) vengono firmate con quella chiave, così ogni APK si installa sopra il
// precedente invece di costringere a disinstallare. Se non c'è, si ripiega sulla
// chiave di debug generata al volo e la build funziona comunque.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

// Attenzione: GitHub Actions definisce le variabili dei secret mancanti come
// stringa VUOTA, non le lascia assenti. Un controllo su != null direbbe di sì
// anche quando la chiave non c'è, e la build fallirebbe cercando un file
// inesistente: per questo si scartano anche i valori vuoti.
fun impostazione(variabile: String, proprieta: String): String? =
    System.getenv(variabile)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(proprieta)?.takeIf { it.isNotBlank() }

val percorsoKeystore = impostazione("KEYSTORE_FILE", "storeFile")
val passwordKeystore = impostazione("KEYSTORE_PASSWORD", "storePassword")
val aliasChiave = impostazione("KEY_ALIAS", "keyAlias")
val passwordChiave = impostazione("KEY_PASSWORD", "keyPassword")

val firmaDisponibile: Boolean = run {
    val percorso = percorsoKeystore ?: return@run false
    passwordKeystore != null &&
        aliasChiave != null &&
        passwordChiave != null &&
        file(percorso).exists()
}

android {
    namespace = "com.kidstracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kidstracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.4.5"
    }

    signingConfigs {
        if (firmaDisponibile) {
            create("rilascio") {
                storeFile = file(percorsoKeystore!!)
                storePassword = passwordKeystore
                keyAlias = aliasChiave
                keyPassword = passwordChiave
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            if (firmaDisponibile) {
                signingConfig = signingConfigs.getByName("rilascio")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (firmaDisponibile) {
                signingConfig = signingConfigs.getByName("rilascio")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
