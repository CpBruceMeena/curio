# Curio ProGuard Rules

# Preserve generic type signatures (required by Gson for parameterized types like List<Content>)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface com.curio.app.data.api.*

# Keep Gson serialized model classes (entire class + all members + generic signatures)
-keep class com.curio.app.data.model.** { *; }

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Material3
-dontwarn androidx.compose.material3.**

# Keep Retrofit internals
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep OkHttp logging
-dontwarn okhttp3.internal.**
