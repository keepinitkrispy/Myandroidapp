# Add project specific ProGuard rules here.

# Keep GDELT data models (Gson needs them)
-keep class com.osint.situationroom.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# OSMdroid
-dontwarn org.osmdroid.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
