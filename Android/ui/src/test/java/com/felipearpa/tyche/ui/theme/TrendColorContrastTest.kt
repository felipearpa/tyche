package com.felipearpa.tyche.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The trend colors and `TrendIndicator`'s qualifier text stay readable (WCAG 4.5:1) on every
 * surface the indicator appears on: the screen canvas, the History and Timeline badge fill
 * (`surfaceVariant`), and the leaderboard's current-user row.
 */
class TrendColorContrastTest {
    @Test
    fun `trend colors match the values shared with iOS`() {
        lightExtendedColorScheme.gain shouldBe Color(0xFF33691E)
        lightExtendedColorScheme.drop shouldBe Color(0xFFB71C1C)
        lightExtendedColorScheme.steady shouldBe Color(0xFF8C5E00)

        darkExtendedColorScheme.gain shouldBe Color(0xFFA5D6A7)
        darkExtendedColorScheme.drop shouldBe Color(0xFFEF9A9A)
        darkExtendedColorScheme.steady shouldBe Color(0xFFFFEB3B)
    }

    @Test
    fun `every trend color meets normal-text contrast on every trend surface`() {
        themes.forEach { (name, scheme, extended) ->
            surfaces(scheme, extended).forEach { (surfaceName, surface) ->
                listOf("gain" to extended.gain, "drop" to extended.drop, "steady" to extended.steady)
                    .forEach { (colorName, color) ->
                        withClue("$name $colorName on $surfaceName") {
                            contrastRatio(color, surface).shouldBeGreaterThanOrEqual(MINIMUM_TEXT_CONTRAST)
                        }
                    }
            }
        }
    }

    @Test
    fun `the qualifier and state labels meet normal-text contrast on every trend surface`() {
        themes.forEach { (name, scheme, extended) ->
            surfaces(scheme, extended).forEach { (surfaceName, surface) ->
                val label = scheme.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA).compositeOver(surface)
                withClue("$name label on $surfaceName") {
                    contrastRatio(label, surface).shouldBeGreaterThanOrEqual(MINIMUM_TEXT_CONTRAST)
                }
            }
        }
    }

    private fun surfaces(scheme: ColorScheme, extended: ExtendedColorScheme) = listOf(
        "canvas" to scheme.surface,
        "badge fill" to scheme.surfaceVariant,
        "current-user row" to extended.currentUserContainer,
    )

    private val themes = listOf(
        Triple("light", lightColorScheme, lightExtendedColorScheme),
        Triple("dark", darkColorScheme, darkExtendedColorScheme),
    )
}

private const val MINIMUM_TEXT_CONTRAST = 4.5

/** `TrendIndicator`'s label alpha over `onSurface`. */
private const val SECONDARY_TEXT_ALPHA = 0.7f
