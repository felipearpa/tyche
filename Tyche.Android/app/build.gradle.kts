val composeCompilerVersion: String by rootProject.extra

val projectCompileSdk: String by project
val projectMinSdk: String by project

val urlBasePath: String by project

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.com.google.dagger.hilt.android)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
    alias(libs.plugins.google.services)
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
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
    implementation(libs.gson)

    implementation(libs.pull.placeholder)
    implementation(libs.pull.refresh)

    implementation(libs.dagger.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.dagger.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.compose.test)

    debugImplementation(libs.bundles.compose.debug.test)

    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.io.mockk)
    androidTestImplementation(libs.io.mockk.android)
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
}

kapt {
    correctErrorTypes = true
}