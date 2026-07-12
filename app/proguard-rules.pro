# Proguard rules for Tally

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tally.app.**$$serializer { *; }
-keepclassmembers class com.tally.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.tally.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Supabase + Ktor
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
