plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.tally.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tally.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Simkl API key — set this in local.properties or build config
        buildConfigField("String", "SIMKL_CLIENT_ID", "\"${project.findProperty("SIMKL_CLIENT_ID") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ── Compose BOM ──────────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Core Android ─────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // ── Navigation ───────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ── Hilt (dependency injection) ──────────────────────────────
    implementation("com.google.dagger:hilt-android:2.58")
    ksp("com.google.dagger:hilt-compiler:2.58")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Room (local SQLite database) ────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Retrofit + OkHttp (networking for Simkl API) ────────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── Kotlinx Serialization (JSON parsing) ────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // ── Coil (image loading for posters) ────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Supabase Kotlin SDK (Auth + database) ───────────────────
    implementation(platform("io.github.jan-tennert.supabase:bom:3.5.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.0.2")


    // ── Google Play Services (for Google Sign-In) ───────────────
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ── Coroutines ───────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
