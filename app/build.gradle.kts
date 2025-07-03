plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hdekker.opencv_on_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hdekker.opencv_on_android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    val cameraxVersion = "1.3.1" // Or the latest stable version (check https://developer.android.com/jetpack/androidx/releases/camera-x)

    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}") // <<< THIS IS LIKELY MISSING
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-video:${cameraxVersion}") // If you use video capture
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}") // For extensions like Bokeh, HDR, etc. (optional)

    implementation(libs.opencv)
    implementation(libs.reactor.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.camera.core)
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)
    testImplementation(libs.junit)
    androidTestImplementation(libs.core.ktx) // Or "androidx.test:core-ktx:1.5.0"
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.rules) // Or "androidx.test:rules:1.5.0"

}