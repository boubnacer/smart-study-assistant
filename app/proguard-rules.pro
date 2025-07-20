# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Retrofit
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Keep PDFBox
-keep class org.apache.pdfbox.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }

# Keep GSON
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# Keep OpenAI models
-keep class com.example.smartstudyassistant.api.OpenAIRequest { *; }
-keep class com.example.smartstudyassistant.api.OpenAIResponse { *; }

# Keep JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**