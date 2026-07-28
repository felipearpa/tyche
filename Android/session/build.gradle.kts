import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val projectCompileSdk: String by project
val projectMinSdk: String by project

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.felipearpa.tyche.session"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig {
        minSdk = projectMinSdk.toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        jniLibs {
            excludes.add("META-INF/*")
        }
        resources {
            excludes.add("META-INF/*")
        }
    }
    testOptions {
        unitTests.all { test ->
            test.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.bundles.ktor)
    implementation(libs.ktor.client.auth)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.felipearpa.foundation)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.io.mockk)
    testImplementation(libs.kotest.assertions.core)

    testRuntimeOnly(libs.bundles.junit5.runtime)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":network:core"))
}
