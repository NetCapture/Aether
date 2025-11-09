# Add project specific ProGuard rules here.

# UtilCode library rules
-keep class com.blankj.utilcode.** { *; }
-dontwarn com.blankj.utilcode.**

# NetBare library rules
-keep class com.github.megatronking.netbare.** { *; }
-dontwarn com.github.megatronking.netbare.**

# GreenDAO rules
-keep class org.greenrobot.greendao.** { *; }
-keep class cn.demo.appq.greendao.** { *; }

# Entity classes
-keep class cn.demo.appq.entity.** { *; }

# Hidden API access (Android 9+)
# Allow reflection for utilcode
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep the Application class
-keep public class * extends android.app.Application

# If your project uses WebView with JS
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

# Enable obfuscation for release builds
#-dontobfuscate
