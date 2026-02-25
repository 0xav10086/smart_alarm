pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
        maven { url = uri("https://jitpack.io") }
        versionCatalogs {
            create("libs")
        }
    }
}

rootProject.name = "SmartAlarm"
include(":app")