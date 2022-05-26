package com.pipel.tyche.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

val Purple200 = Color(0xFFBB86FC)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

val nord0 = Color(0xFF2E3440)
val nord1 = Color(0xFF3B4252)
val nord2 = Color(0xFF434C5E)
val nord3 = Color(0xFF4C566A)

val nord4 = Color(0xFFD8DEE9)
val nord5 = Color(0xFFE5E9F0)
val nord6 = Color(0xFFECEFF4)

val nord7 = Color(0xFF8FBCBB)
val nord8 = Color(0xFF88C0D0)
val nord9 = Color(0xFF81A1C1)
val nord10 = Color(0xFF5E81AC)

val nord11 = Color(0xFFBF616A)
val nord12 = Color(0xFFD08770)
val nord13 = Color(0xFFEBCB8B)
val nord14 = Color(0xFFA3BE8C)
val nord15 = Color(0xFFB48EAD)

val nord9Variant = Color(0xFF527391)
val nord10Variant = Color(0xFF2E557D)

val Colors.appTopBar: Color
    get() = if (isLight) nord5 else nord5

val Colors.onAppTopBar: Color
    get() = if (isLight) nord0 else nord0

val Colors.onPrimaryLight: Color
    get() = if (isLight) nord0.copy(alpha = 0.54f) else nord0.copy(alpha = 0.54f)

val Colors.stableIndicator: Color
    get() = if (isLight) nord12 else nord12

val Colors.upIndicator: Color
    get() = if (isLight) nord14 else nord14

val Colors.downIndicator: Color
    get() = if (isLight) nord11 else nord11