# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class it.bosler.polyphoneme.**$$serializer { *; }
-keepclassmembers class it.bosler.polyphoneme.** { *** Companion; }
-keepclasseswithmembers class it.bosler.polyphoneme.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep epub4j — kxml2 bundles xmlpull classes that conflict with Android platform
-dontwarn org.xmlpull.**
-dontwarn org.kxml2.**
-dontwarn javax.xml.**
-keep class io.documentnode.epub4j.** { *; }
-keep class org.kxml2.** { *; }

# Jsoup optional re2j dependency
-dontwarn com.google.re2j.**

# Keep ONNX Runtime
-keep class ai.onnxruntime.** { *; }
