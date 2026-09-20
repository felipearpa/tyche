package com.felipearpa.tyche.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColorScheme(
    val gain: Color,
    val drop: Color,
    val steady: Color,
    val placeholder: Color,
    val loadingBackground: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val currentUser: Color,
    val onCurrentUser: Color,
    val currentUserContainer: Color,
    val onCurrentUserContainer: Color,
)

internal val lightExtendedColorScheme = ExtendedColorScheme(
    gain = lightGainColor,
    drop = lightDropColor,
    steady = lightSteadyColor,
    placeholder = lightPlaceholderColor,
    loadingBackground = lightLoadingBackgroundColor,
    warningContainer = lightWarningContainer,
    onWarningContainer = lightOnWarningContainer,
    successContainer = lightSuccessContainer,
    onSuccessContainer = lightOnSuccessContainer,
    currentUser = lightCurrentUser,
    onCurrentUser = lightOnCurrentUser,
    currentUserContainer = lightCurrentUserContainer,
    onCurrentUserContainer = lightOnCurrentUserContainer,
)

internal val darkExtendedColorScheme = ExtendedColorScheme(
    gain = darkGainColor,
    drop = darkDropColor,
    steady = darkSteadyColor,
    placeholder = darkPlaceholderColor,
    loadingBackground = darkLoadingBackgroundColor,
    warningContainer = darkWarningContainer,
    onWarningContainer = darkOnWarningContainer,
    successContainer = darkSuccessContainer,
    onSuccessContainer = darkOnSuccessContainer,
    currentUser = darkCurrentUser,
    onCurrentUser = darkOnCurrentUser,
    currentUserContainer = darkCurrentUserContainer,
    onCurrentUserContainer = darkOnCurrentUserContainer,
)

val LocalExtendedColorScheme = staticCompositionLocalOf { lightExtendedColorScheme }
