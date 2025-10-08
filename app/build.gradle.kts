plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hdekker.opencv_on_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hdekker.opencv_on_android"
        minSdk = 26
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

    implementation("com.hdekker:opencv-02-ball-detection:1.0.0-SNAPSHOT"){
        exclude(group = "org.openpnp", module = "opencv")
        exclude(group = "org.springframework")
        exclude(group = "io.netty")
        exclude(group = "ch.qos.logback")
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.springframework.boot")
    }
    implementation(libs.slf4j.android) // Or the latest version
    implementation(libs.slf4j.api) // Or the latest version
    implementation(libs.jackson.databind)// Check for the latest version
    // In app/build.gradle.kts dependencies block
    implementation(libs.jackson.module.parameter.names) // Use the latest Jackson version
    implementation(libs.jackson.module.android.record)
    implementation(libs.opencv.v490)
    implementation(libs.reactor.core.v360) // Check for the latest version

    implementation(libs.androidx.junit)
    val cameraxVersion = "1.3.1" // Or the latest stable version (check https://developer.android.com/jetpack/androidx/releases/camera-x)

    implementation(libs.androidx.camera.core.v131)
    implementation(libs.androidx.camera.camera2) // <<< THIS IS LIKELY MISSING
    implementation(libs.androidx.camera.lifecycle.v131)
    implementation(libs.androidx.camera.video) // If you use video capture
    implementation(libs.androidx.camera.view.v131)
    implementation(libs.androidx.camera.extensions) // For extensions like Bokeh, HDR, etc. (optional)

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