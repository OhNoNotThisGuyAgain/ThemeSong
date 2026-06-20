# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.spotzones.**$$serializer { *; }
-keepclassmembers class com.spotzones.** {
    *** Companion;
}

# Keep serializable model classes
-keep @kotlinx.serialization.Serializable class com.spotzones.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Spotify SDK
-keep class com.spotify.** { *; }
-dontwarn com.spotify.**

# Hilt / Dagger generated code is handled by the plugin.

# Keep Timber line numbers in release stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
