// The Android Gradle Plugin is only put on the classpath when Android modules are part of the
// build (see settings.gradle.kts, -PjvmOnly=true skips them) so JVM-only builds don't need
// Google's Maven repository.
buildscript {
    val jvmOnly = providers.gradleProperty("jvmOnly").map { it.toBoolean() }.getOrElse(false)
    if (!jvmOnly) {
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath("com.android.tools.build:gradle:8.7.3")
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
