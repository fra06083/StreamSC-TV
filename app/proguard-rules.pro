# Keep QuickJS JNI bindings
-keep class com.dokar.quickjs.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
# Keep provider models used via reflection / JSON
-keep class it.fra06083.streamsctv.data.** { *; }
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
# Media3
-dontwarn androidx.media3.**
# Jsoup
-keep class org.jsoup.** { *; }
