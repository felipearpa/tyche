package com.felipearpa.tyche.ui.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PrimaryColorTest {
    @Test
    fun `primary colors match the shared accent values`() {
        lightColorScheme.primary shouldBe Color(0xFF2E7D32)
        lightColorScheme.onPrimary shouldBe Color(0xFFFFFFFF)
        lightColorScheme.primaryContainer shouldBe Color(0xFF1B5E20)
        lightColorScheme.onPrimaryContainer shouldBe Color(0xFFFFFFFF)

        darkColorScheme.primary shouldBe Color(0xFF4CAF50)
        darkColorScheme.onPrimary shouldBe Color(0xFF000000)
        darkColorScheme.primaryContainer shouldBe Color(0xFF1B5E20)
        darkColorScheme.onPrimaryContainer shouldBe Color(0xFFFFFFFF)
    }

    @Test
    fun `every primary foreground pair meets normal-text contrast`() {
        listOf(lightColorScheme, darkColorScheme).forEach { scheme ->
            contrastRatio(scheme.onPrimary, scheme.primary)
                .shouldBeGreaterThanOrEqual(4.5)
            contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
                .shouldBeGreaterThanOrEqual(4.5)
        }
    }
}
