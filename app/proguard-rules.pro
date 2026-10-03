# kotlinx.serialization: keep generated serializers of the content models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.sobik.** {
    *** Companion;
}
-keepclasseswithmembers class com.sobik.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sobik.**$$serializer { *; }
