plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.tomasrepcik.sensorbox.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testFixtures {
        enable = true
    }
}

dependencies {
    api(project(":core-common"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.datetime)

    testFixturesImplementation(libs.coroutines.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}
