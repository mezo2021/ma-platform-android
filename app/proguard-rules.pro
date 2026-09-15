# Keep WebView and JavaScript interface classes
-keepclasseswithmembernames class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class com.maplatform.app.MainActivity$AndroidBridge {
    public *;
}
