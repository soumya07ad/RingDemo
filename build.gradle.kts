import java.util.Properties

plugins {
    id("com.android.application") version "8.5.0"
    kotlin("android") version "2.1.0"
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.google.gms.google-services") version "4.4.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.dkgs.innerpulse"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("RELEASE_KEYSTORE_PATH") ?: "release.jks")
            storePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.dkgs.innerpulse"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    // Core library desugaring (java.time support for API < 26)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.activity:activity-compose:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    
    // Compose Markdown
    implementation("com.github.jeziellago:compose-markdown:0.5.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // SplashScreen API (Android 12+ compatible)
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // WebView
    implementation("androidx.webkit:webkit:1.8.0")
    
    // Retrofit + Networking
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // DataStore for secure token storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Health Connect (for reading steps, distance, calories from phone sensors)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // ══════════════════════════════════════════════════════════
    // JMRing SDK Integration
    // ══════════════════════════════════════════════════════════
    api("com.jimi:JMRing:1.0.0_13")
    
    // Explicitly adding transitive dependencies to fix "Cannot access supertype" errors in IDE
    api("com.jimi:ycbtsdk-release:4.0.6")
    api("com.jimi:jl_rcsp:0.5.5")
    api("com.jimi:JL_Watch:1.10.3")
    api("com.jimi:jl_bt_ota:1.9.6")
    api("com.jimi:aizo_sdk_debug:1.1.5")
    api("com.jimi:aizo_be_lib_release:1.1.5")
    api("com.jimi:aizo_serversdk_release:2_2.1.14")
    api("com.jimi:AliAgent-release:4.2.2")
    api("com.jimi:BmpConvert:1.6.0")
    
    // External SDK dependencies
    implementation("com.blankj:utilcodex:1.31.1")
    implementation("com.tencent:mmkv-static:1.3.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")
    implementation("com.github.liangjingkanji:Serialize:1.3.1")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
}
