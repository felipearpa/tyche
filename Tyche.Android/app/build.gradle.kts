val projectCompileSdk: String by project
val projectMinSdk: String by project
val urlBasePath: String by project

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.services)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.felipearpa.tyche"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig {
        applicationId = "com.felipearpa.tyche"
        minSdk = projectMinSdk.toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        all {
            buildConfigField(type = "String", name = "URL_BASE_BATH", value = """"$urlBasePath"""")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        jniLibs {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
        resources {
            excludes.addAll(
                listOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "/META-INF/LICENSE.md",
                    "/META-INF/LICENSE-notice.md"
                )
            )
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.material3)
    implementation(libs.paging.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.constraint.layout.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.bundles.retrofit)
    implementation(libs.dagger.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.felipearpa.viewing.state)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.felipearpa.foundation)
    implementation(libs.kotlinx.datetime)

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
    implementation(project(":account"))
    implementation(project(":pool"))
    implementation(project(":bet"))
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":session"))
    implementation(project(":data:pool"))
    implementation(project(":data:bet"))
    implementation(project(":network:core"))
}

kapt {
    correctErrorTypes = true
}
