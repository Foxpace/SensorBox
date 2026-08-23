plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-common"))
    implementation(libs.coroutines.core)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}
