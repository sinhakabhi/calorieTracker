import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Firebase (used for the tester allowlist) is only wired up when app/google-services.json exists,
// so the project still builds without a Firebase project.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// GEMINI_API_KEY lives in local.properties (git-ignored) and is exposed as BuildConfig.GEMINI_API_KEY.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY", "").trim()
// OAuth "Web application" client ID from Google Cloud, used for Sign in with Google. Optional when
// google-services.json provides one (Firebase console → Authentication → Google enabled).
val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "").trim()
// Tester builds: require Google sign-in and an email on the Remote Config allowlist.
val inviteOnly: Boolean = localProperties.getProperty("INVITE_ONLY", "false").trim().toBoolean()

// Release signing key, from keystore.properties (git-ignored). Without it, release builds are unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.veyra.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.veyra.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "2.2"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("boolean", "INVITE_ONLY", inviteOnly.toString())
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Shrink code and resources (mostly unused icons): about 20 MB → a few MB.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.androidx.work.runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // Android's built-in org.json is only a stub in local JVM tests, so tests use the real one.
    testImplementation(libs.org.json)
}
