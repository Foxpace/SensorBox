plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.oss.licenses) apply false
    alias(libs.plugins.screenshot) apply false
}

subprojects {
    plugins.withId("dev.detekt") {
        extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
        }

        dependencies {
            add("detektPlugins", libs.detekt.rules.ktlint)
        }
    }
}

data class ReadmeScreenshot(val module: String, val preview: String, val destination: String)

val readmeScreenshots = listOf(
    ReadmeScreenshot(":app", "readmeSourceSelection", "sensorbox-phone-record.png"),
    ReadmeScreenshot(":app", "readmeMeasurementSetup", "sensorbox-phone-setup.png"),
    ReadmeScreenshot(":app", "readmeIntroWelcome", "sensorbox-intro-welcome.png"),
    ReadmeScreenshot(":app", "readmeIntroPrivacy", "sensorbox-intro-privacy.png"),
    ReadmeScreenshot(":app", "readmeIntroPolicy", "sensorbox-intro-policy.png"),
    ReadmeScreenshot(":app", "readmeIntroLifecycle", "sensorbox-intro-lifecycle.png"),
    ReadmeScreenshot(":app", "readmeIntroBattery", "sensorbox-intro-battery.png"),
    ReadmeScreenshot(":app", "readmeIntroStorage", "sensorbox-intro-storage.png"),
    ReadmeScreenshot(":wear", "readmeWearDashboard", "sensorbox-wear.png"),
    ReadmeScreenshot(":wear", "readmeWearLivePicker", "sensorbox-wear-live.png"),
)

tasks.register("refreshReadmeScreenshots") {
    group = "documentation"
    description = "Renders every README screenshot on the host and copies it into docs/images."
    dependsOn(":app:updateDebugScreenshotTest", ":wear:updateDebugScreenshotTest")

    doLast {
        readmeScreenshots.forEach { screenshot ->
            val references = project(screenshot.module).file("src/screenshotTestDebug/reference")
            val matches = references.walkTopDown().filter { file ->
                file.isFile && file.extension == "png" && file.name.startsWith("${screenshot.preview}_")
            }.toList()
            require(matches.size == 1) {
                "Expected one ${screenshot.preview} reference image, found ${matches.size}."
            }
            matches.single().copyTo(file("docs/images/${screenshot.destination}"), overwrite = true)
        }
    }
}
