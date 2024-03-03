buildscript {
    val composeCompilerVersion by extra { "1.5.10" }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.jetbrainsKotlinxSerialization) apply false
    alias(libs.plugins.jetbrainsKotlinKapt) apply false
    alias(libs.plugins.googleDaggerHiltAndroid) apply false
}
