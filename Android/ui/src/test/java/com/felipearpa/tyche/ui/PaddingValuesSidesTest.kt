package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaddingValuesSidesTest {
    private val screenPadding = PaddingValues(start = 12.dp, top = 64.dp, end = 20.dp, bottom = 48.dp)

    @Test
    fun `given screen padding when the header takes it then every side but the bottom is kept`() {
        screenPadding.excludingBottom().sides(LayoutDirection.Ltr) shouldBe
            listOf(12.dp, 64.dp, 20.dp, 0.dp)
    }

    @Test
    fun `given screen padding when the list takes the bottom then only the bottom is kept`() {
        screenPadding.onlyBottom().sides(LayoutDirection.Ltr) shouldBe
            listOf(0.dp, 0.dp, 0.dp, 48.dp)
    }

    @Test
    fun `given a right-to-left layout when the header takes the padding then start and end follow the direction`() {
        screenPadding.excludingBottom().sides(LayoutDirection.Rtl) shouldBe
            listOf(20.dp, 64.dp, 12.dp, 0.dp)
    }

    @Test
    fun `given the two parts when they are added then they rebuild the screen padding`() {
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            val header = screenPadding.excludingBottom().sides(direction)
            val list = screenPadding.onlyBottom().sides(direction)
            header.zip(list) { a, b -> a + b } shouldBe screenPadding.sides(direction)
        }
    }

    @Test
    fun `given padding that changes when a part is read later then it reflects the current value`() {
        var bottom = 48.dp
        val changing = object : PaddingValues {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
            override fun calculateTopPadding() = 0.dp
            override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
            override fun calculateBottomPadding() = bottom
        }
        val listPart = changing.onlyBottom()

        bottom = 0.dp

        listPart.calculateBottomPadding() shouldBe 0.dp
    }

    @Test
    fun `given a keyboard taller than the bottom bar when the list pads for what shows then the bottom is zero`() {
        screenPadding.bottomUncoveredBy(keyboard(bottom = 300.dp), Density(density = 2f))
            .sides(LayoutDirection.Ltr) shouldBe listOf(12.dp, 64.dp, 20.dp, 0.dp)
    }

    @Test
    fun `given a keyboard shorter than the bottom bar when the list pads for what shows then only the uncovered part remains`() {
        screenPadding.bottomUncoveredBy(keyboard(bottom = 30.dp), Density(density = 2f))
            .calculateBottomPadding() shouldBe 18.dp
    }

    @Test
    fun `given no keyboard when the list pads for what shows then the screen padding is unchanged`() {
        screenPadding.bottomUncoveredBy(keyboard(bottom = 0.dp), Density(density = 2f))
            .sides(LayoutDirection.Ltr) shouldBe screenPadding.sides(LayoutDirection.Ltr)
    }

    private fun keyboard(bottom: Dp) = WindowInsets(bottom = bottom)

    /** Left, top, right, bottom, resolved for [direction]. */
    private fun PaddingValues.sides(direction: LayoutDirection): List<Dp> = listOf(
        calculateLeftPadding(direction),
        calculateTopPadding(),
        calculateRightPadding(direction),
        calculateBottomPadding(),
    )
}
