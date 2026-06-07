# Curio ProGuard Rules

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.curio.app.data.api.*

# Keep Gson serialized model classes
-keepclassmembers class com.curio.app.data.model.** {
    <fields>;
}

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Material3
-dontwarn androidx.compose.material3.**
