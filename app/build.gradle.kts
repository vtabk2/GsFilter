plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.gsfilter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gsfilter"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":filter"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.glide)
    ksp(libs.glide.ksp)

    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.androidsvg)
    implementation(libs.mlkit.face)

    testImplementation(libs.junit)
}
