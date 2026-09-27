package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Splits a screen's padding when a fixed header sits between the top app bar and a list:
 * the header's container takes [excludingBottom] and the list takes [onlyBottom] as its
 * content padding, so the list viewport still reaches the bottom window edge.
 *
 * Both read the source lazily, so scaffold padding that follows a collapsing app bar
 * keeps updating layout without recomposing the caller.
 */
@Stable
fun PaddingValues.excludingBottom(): PaddingValues = SidesOf(source = this, bottom = false)

/** The bottom edge of this padding alone. See [excludingBottom]. */
@Stable
fun PaddingValues.onlyBottom(): PaddingValues =
    SidesOf(source = this, horizontal = false, top = false)

/**
 * This padding with its bottom reduced by the part that [insets] already cover, never below
 * zero. Use it for a list whose screen padding includes a bottom bar that stays at the window
 * edge while the keyboard covers it: with the list fitted above the keyboard, only the part of
 * the bar that still shows above the keyboard needs padding, so the two heights do not stack.
 *
 * The insets are read lazily, so the padding follows the keyboard animation during layout.
 */
@Stable
fun PaddingValues.bottomUncoveredBy(insets: WindowInsets, density: Density): PaddingValues =
    BottomUncoveredBy(source = this, insets = insets, density = density)

private data class BottomUncoveredBy(
    private val source: PaddingValues,
    private val insets: WindowInsets,
    private val density: Density,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        source.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = source.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        source.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp {
        val covered = with(density) { insets.getBottom(this).toDp() }
        return (source.calculateBottomPadding() - covered).coerceAtLeast(0.dp)
    }
}

private data class SidesOf(
    private val source: PaddingValues,
    private val horizontal: Boolean = true,
    private val top: Boolean = true,
    private val bottom: Boolean = true,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        if (horizontal) source.calculateLeftPadding(layoutDirection) else 0.dp

    override fun calculateTopPadding(): Dp = if (top) source.calculateTopPadding() else 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        if (horizontal) source.calculateRightPadding(layoutDirection) else 0.dp

    override fun calculateBottomPadding(): Dp = if (bottom) source.calculateBottomPadding() else 0.dp
}
