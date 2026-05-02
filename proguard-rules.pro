# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
# optimization is turned off with -dontoptimize flag in the script.

# Keep WebView classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep your app's classes
-keep class com.dkgs.innerpulse.** { *; }
-keepnames class com.dkgs.innerpulse.** { *; }

# ══════════════════════════════════════════════════════════
# JMRing SDK & Dependencies (DO NOT OBFUSCATE)
# ══════════════════════════════════════════════════════════

# JMRing Official SDK
-keep class com.jimi.** { *; }
-keep class com.gps.track.jmring.** { *; }

# Jieli (BT/OTA Library)
-keep class com.yucheng.** { *; }
-keep class com.jieli.** { *; }

# Tencent MMKV
-keep class com.tencent.mmkv.** { *; }

# Utils & Libraries used by SDK
-keep class com.blankj.utilcode.** { *; }
-keep class io.reactivex.rxjava3.** { *; }
-keep class org.eclipse.paho.** { *; }

# Keep native methods and names
-keepclasseswithmembernames class * {
    native <methods>;
}

# ══════════════════════════════════════════════════════════
# Android & Jetpack
# ══════════════════════════════════════════════════════════
-keep class androidx.health.connect.client.** { *; }
-keep class com.google.android.gms.auth.** { *; }

