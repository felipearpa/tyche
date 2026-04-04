import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val projectCompileSdk: String by project
val projectMinSdk: String by project

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.dagger.hilt)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.felipearpa.tyche.data.pool"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig {
        minSdk = projectMinSdk.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    packaging {
        jniLibs {
            excludes.add("META-INF/*")
        }
        resources {
            excludes.add("META-INF/*")
        }
    }
}

dependencies {
    implementation(libs.bundles.ktor)
    implementation(libs.dagger.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.io.mockk)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.io.mockk.android)

    ksp(libs.dagger.hilt.compiler)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":session"))
    implementation(project(":network:core"))
}
