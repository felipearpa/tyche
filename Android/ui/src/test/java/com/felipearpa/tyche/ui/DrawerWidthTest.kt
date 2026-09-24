package com.felipearpa.tyche.ui

import androidx.compose.ui.unit.dp
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DrawerWidthTest {
    @Test
    fun `given a compact window when sizing the drawer then it takes 85 percent of the width`() {
        drawerWidth(maxWidth = 360.dp).value shouldBe (306f plusOrMinus TOLERANCE)
    }

    @Test
    fun `given a phone window where 85 percent passes the bound when sizing the drawer then it stops at the readable maximum`() {
        drawerWidth(maxWidth = 448.dp).value shouldBe (MaximumDrawerWidth.value plusOrMinus TOLERANCE)
    }

    @Test
    fun `given a wide window when sizing the drawer then it keeps the readable maximum instead of filling most of the window`() {
        drawerWidth(maxWidth = 1280.dp).value shouldBe (360f plusOrMinus TOLERANCE)
    }

    @Test
    fun `given a leading inset when sizing a wide drawer then the inset is added so the padded content keeps the readable maximum`() {
        drawerWidth(maxWidth = 997.dp, leadingInset = 48.dp).value shouldBe (408f plusOrMinus TOLERANCE)
    }

    @Test
    fun `given a narrow window when sizing the drawer then a touch target's width of the pushed screen stays visible`() {
        val maxWidth = 280.dp

        (maxWidth - drawerWidth(maxWidth = maxWidth)).value shouldBe
            (MinimumDismissalWidth.value plusOrMinus TOLERANCE)
    }

    @Test
    fun `given a trailing inset when sizing a narrow drawer then the visible strip ends where the inset begins`() {
        val maxWidth = 280.dp
        val trailingInset = 24.dp

        (maxWidth - trailingInset - drawerWidth(maxWidth = maxWidth, trailingInset = trailingInset)).value shouldBe
            (MinimumDismissalWidth.value plusOrMinus TOLERANCE)
    }

    @Test
    fun `given windows from zero to tablet width when sizing the drawer then it is never negative and never leaves less than the strip`() {
        (0..1600 step 10).map { it.dp }.forEach { maxWidth ->
            val width = drawerWidth(maxWidth = maxWidth)

            width.value shouldBeGreaterThanOrEqual 0f
            if (width > 0.dp) {
                (maxWidth - width).value shouldBeGreaterThanOrEqual MinimumDismissalWidth.value - TOLERANCE
            }
        }
    }
}

private const val TOLERANCE = 0.01f
