plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.oss.licenses) apply false
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
