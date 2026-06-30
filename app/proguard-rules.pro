# Circuit Break ProGuard rules

# Keep Gson models
-keep class com.circuitbreak.app.data.ActivityItem { *; }

# Keep JavaScript interface
-keepclassmembers class com.circuitbreak.app.MainActivity$CircuitBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep enum
-keep class com.circuitbreak.app.data.ItemType { *; }
