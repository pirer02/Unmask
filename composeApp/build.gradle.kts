import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.0.0"

    id("com.google.gms.google-services")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            implementation("io.ktor:ktor-client-okhttp:2.3.11")

            // Lo que añadiste en el paso anterior:
            implementation("com.google.android.gms:play-services-auth:21.0.0")
            implementation("com.google.firebase:firebase-auth-ktx:22.3.1")

            // 👇 AÑADE ESTAS DOS LÍNEAS NUEVAS AQUÍ 👇
            // Esto dicta las versiones compatibles de Firebase para Android
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:32.8.1"))
            // Declaramos firestore nativo (sin versión, porque el BoM ya se encarga)
            implementation("com.google.firebase:firebase-firestore")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(compose.materialIconsExtended)

            implementation("media.kamel:kamel-image:0.9.4")

            implementation("io.ktor:ktor-client-core:2.3.11")


            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")


            implementation("dev.gitlive:firebase-auth:1.11.1") // Autenticación
            implementation("dev.gitlive:firebase-firestore:1.11.1") // Base de datos (opcional)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

