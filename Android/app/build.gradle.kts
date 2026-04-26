import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val projectCompileSdk: String by project
val projectMinSdk: String by project
val urlBasePath: String by project
val signInLinkUrlTemplate: String by project
val joinPoolUrlTemplate: String by project
val iosBundleId: String by project

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.services)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.felipearpa.tyche"
    compileSdk = projectCompileSdk.toInt()
    defaultConfig {
        applicationId = "com.felipearpa.fortuna"
        minSdk = projectMinSdk.toInt()
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        all {
            buildConfigField(type = "String", name = "URL_BASE_BATH", value = """"$urlBasePath"""")
            buildConfigField(type = "String", name = "SIGN_IN_LINK_URL_TEMPLATE", value = """"$signInLinkUrlTemplate"""")
            buildConfigField(type = "String", name = "JOIN_POOL_URL_TEMPLATE", value = """"$joinPoolUrlTemplate"""")
            buildConfigField(type = "String", name = "IOS_BUNDLE_ID", value = """"$iosBundleId"""")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.material3)
    implementation(libs.paging.compose)
    implementation(libs.navigation.compose)
    implementation(libs.constraint.layout.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.bundles.ktor)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
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
