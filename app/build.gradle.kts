plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cadusmart"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cadusmart"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Emulador: 10.0.2.2 = el localhost de tu PC.
            // Celular real: cambia por la IP de tu PC, p. ej. "http://192.168.1.15:3000/" emulator "http://10.0.2.2:3000/\
            buildConfigField("String", "BASE_URL", "\"http://localhost:3000/\"")
        }
        release {
            // Cuando publiques el backend, pon aquí su URL https (debe terminar en "/")
            buildConfigField("String", "BASE_URL", "\"https://TU-BACKEND.onrender.com/\"")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Sprints 2 y 3
    implementation(libs.androidx.work)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navegación, lifecycle e íconos
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.viewmodel.compose)

    // Sprint 1: red y sesión
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore)

    // Sprints 2 y 3 (déjalas comentadas hasta usarlas)
    // implementation(libs.androidx.work)
    // implementation(libs.androidx.hilt.work)
    // ksp(libs.androidx.hilt.compiler)
    // implementation(libs.camerax.core)
    // implementation(libs.camerax.camera2)
    // implementation(libs.camerax.lifecycle)
    // implementation(libs.camerax.view)
    // implementation(libs.mlkit.barcode)
}