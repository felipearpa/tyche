val projectCompileSdk: String by project
val projectMinSdk: String by project

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.com.google.dagger.hilt.android)
    alias(libs.plugins.org.jetbrains.kotlinx.serialization)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
}

android {
    namespace = "com.felipearpa.data.pool"
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

    kotlinOptions {
        jvmTarget = "17"
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
    implementation(libs.bundles.retrofit)
    implementation(libs.gson)

    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)

    implementation(libs.kotlinx.serialization)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.io.mockk)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":session"))
}

kapt {
    correctErrorTypes = true
}