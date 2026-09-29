package com.felipearpa.tyche.ui.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CurrentUserColorTest {
    @Test
    fun `current-user colors match the approved light and dark values`() {
        lightExtendedColorScheme.currentUser shouldBe Color(0xFF176B3A)
        lightExtendedColorScheme.onCurrentUser shouldBe Color(0xFFFFFFFF)
        lightExtendedColorScheme.currentUserContainer shouldBe Color(0xFFD4F3DE)
        lightExtendedColorScheme.onCurrentUserContainer shouldBe Color(0xFF176B3A)

        darkExtendedColorScheme.currentUser shouldBe Color(0xFF7EDB9A)
        darkExtendedColorScheme.onCurrentUser shouldBe Color(0xFF12351F)
        darkExtendedColorScheme.currentUserContainer shouldBe Color(0xFF143C24)
        darkExtendedColorScheme.onCurrentUserContainer shouldBe Color(0xFFB3F0C3)
    }

    @Test
    fun `every current-user foreground pair meets normal-text contrast`() {
        listOf(lightExtendedColorScheme, darkExtendedColorScheme).forEach { scheme ->
            contrastRatio(scheme.onCurrentUser, scheme.currentUser)
                .shouldBeGreaterThanOrEqual(4.5)
            contrastRatio(scheme.onCurrentUserContainer, scheme.currentUserContainer)
                .shouldBeGreaterThanOrEqual(4.5)
        }
    }
}
