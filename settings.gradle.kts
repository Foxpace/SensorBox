pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.android.gms.oss-licenses-plugin") {
                useModule("com.google.android.gms:oss-licenses-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SensorBox"

include(
    ":app",
    ":core",
    ":core-common",
    ":recording-core",
    ":sensorservices",
    ":wear",
    ":wearoslib",
)

project(":wearoslib").projectDir = file("WearOsLib")
