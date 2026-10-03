pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sobik"

// Pure Kotlin/JVM modules: domain, engine, solvers, scanner core, content, data.
// They build and test without the Android SDK.
include(
    ":core-model",
    ":cube-engine",
    ":solver-2x2",
    ":solver-3x3",
    ":scanner",
    ":content",
    ":data",
)

// Android modules. Pass -PjvmOnly=true to build/test only the JVM modules
// (e.g. on a CI runner without an Android SDK).
if (!providers.gradleProperty("jvmOnly").map { it.toBoolean() }.getOrElse(false)) {
    include(
        ":visualization",
        ":scanner-camerax",
        ":app",
    )
}
