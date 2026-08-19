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
-keep class org.bouncycastle.** { *; }
-keep class com.mmc.** { *; }
-keep class com.mdm.** { *; }

# ============================================================================
# Missing classes (R8 build failure)
# ============================================================================

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.**

-dontwarn org.slf4j.**

-dontwarn java.beans.**
-dontwarn javax.xml.stream.**
-dontwarn com.fasterxml.jackson.databind.ext.**

-dontwarn javax.annotation.**
-dontwarn javax.activation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# ============================================================================
# Keep rules -- these do NOT fail the build; they fail silently at RUNTIME.
# ============================================================================
-keep class io.android.sbi.dto.** { *; }
-keepclassmembers class io.android.sbi.dto.** { <fields>; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

-keep class org.jose4j.** { *; }
-dontwarn org.jose4j.**

-keepclasseswithmembernames class * {
    native <methods>;
}
# ============================================================================
# Third-party SDKs with native (JNI) code.
# ============================================================================

-keep class com.phoenixcapture.** { *; }
-keep class net.idrnd.** { *; }
-keep class ai.tech5.** { *; }
-keep class in.nprime.jp2.** { *; }
-keep class nprime.in.jp2library.** { *; }

-dontwarn com.phoenixcapture.**
-dontwarn net.idrnd.**
-dontwarn ai.tech5.**

-dontobfuscate

-keep class * extends com.fasterxml.jackson.core.type.TypeReference { *; }
