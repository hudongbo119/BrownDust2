# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep OpenCV classes
-keep class org.opencv.** { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes used with Gson
-keep class com.example.browndustbot.TaskConfig { *; }
-keep class com.example.browndustbot.TaskStep { *; }
-keep class com.example.browndustbot.TextMatchConfig { *; }
-keep class com.example.browndustbot.SerializableRect { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
