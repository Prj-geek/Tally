plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.0" apply false
    id("com.google.dagger.hilt.android") version "2.58" apply false
    // Firebase google-services plugin will be added in Phase 3 for FCM
}
