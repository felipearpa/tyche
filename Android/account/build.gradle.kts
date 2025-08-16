val projectCompileSdk: String by project
val projectMinSdk: String by project

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.felipearpa.tyche.account"
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
    implementation(libs.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.material3)
    implementation(libs.paging.compose)
    implementation(libs.navigation.compose)
    implementation(libs.bundles.retrofit)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.felipearpa.viewing.state)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.felipearpa.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.io.mockk)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.io.mockk.android)

    debugImplementation(libs.bundles.compose.debug.test)

    kapt(libs.dagger.hilt.compiler)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":session"))
}

kapt {
    correctErrorTypes = true
}
