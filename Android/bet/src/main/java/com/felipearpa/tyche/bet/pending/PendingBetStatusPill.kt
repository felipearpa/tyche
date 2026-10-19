package com.felipearpa.tyche.bet.pending

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import com.felipearpa.tyche.ui.R as SharedR

/** The status as the pill shows it and assistive technology reads it; Saving while the ball spins. */
@get:StringRes
internal val RowState.statusText: Int
    get() = when (this) {
        RowState.LOCKED -> R.string.bet_status_locked
        RowState.EMPTY -> R.string.bet_status_no_bet
        RowState.INCOMPLETE -> R.string.bet_status_incomplete
        RowState.SAVING -> R.string.bet_saving_accessibility_value
        RowState.SAVED -> R.string.bet_status_saved
        RowState.FAILED -> R.string.bet_status_not_saved
    }

@get:DrawableRes
private val RowState.glyph: Int?
    get() = when (this) {
        RowState.LOCKED -> SharedR.drawable.lock
        RowState.SAVED -> SharedR.drawable.done
        RowState.FAILED -> SharedR.drawable.error
        RowState.EMPTY, RowState.INCOMPLETE, RowState.SAVING -> null
    }

/**
 * The card's trailing status: one pill anchored to the trailing edge, as tall as one line of its
 * text and never shorter than 26 dp, in every state. No bet and Incomplete show text, Saved, Not
 * saved, and Locked an icon and text, and Saving the shared ball spinner.
 *
 * A state change is one transition: the old content fades out over the first part of it and the
 * new one fades in after, while the pill's width eases to the new content's with its trailing
 * edge fixed and its fill interpolates. With [reduceMotion] the width changes at once.
 *
 * The card's summary carries the status as its state description, so the pill is hidden from
 * TalkBack, except that it speaks [announcement] as a polite live region when the card reports
 * one (a save completing or failing, or the lock).
 */
@Composable
internal fun PendingBetStatusPill(
    rowState: RowState,
    isPlaceholder: Boolean,
    announcement: String?,
    modifier: Modifier = Modifier,
) {
    val colors = pendingBetColors()
    val reduceMotion = reduceMotion()
    val fill by animateColorAsState(
        targetValue = if (isPlaceholder) Color.Transparent else rowState.fill(colors),
        animationSpec = PendingBetStyle.spec(),
        label = "statusFill",
    )
    val outline by animateColorAsState(
        targetValue = if (rowState == RowState.EMPTY && !isPlaceholder) colors.emptyOutline else Color.Transparent,
        animationSpec = PendingBetStyle.spec(),
        label = "statusOutline",
    )
    val height = pendingBetStatusHeight()

    AnimatedContent(
        targetState = rowState,
        transitionSpec = { PendingBetStyle.contentSwap(reduceMotion) },
        contentAlignment = Alignment.CenterEnd,
        label = "statusPill",
        modifier = modifier
            .testTag(PENDING_BET_STATUS_TAG)
            .height(height)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, outline, CircleShape)
            .statusSemantics(announcement),
    ) { state ->
        PendingBetStatusContent(rowState = state, isPlaceholder = isPlaceholder, height = height)
    }
}

/** Finds the status pill in UI tests. */
internal const val PENDING_BET_STATUS_TAG = "pendingBetStatus"

/** The pill's height: 26 dp, grown with the font scale like the text inside it. */
@Composable
internal fun pendingBetStatusHeight(): Dp = PendingBetStyle.pillHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)

private fun Modifier.statusSemantics(announcement: String?): Modifier =
    clearAndSetSemantics {
        if (announcement != null) {
            contentDescription = announcement
            liveRegion = LiveRegionMode.Polite
        }
    }

private fun RowState.fill(colors: PendingBetColors): Color = when (this) {
    RowState.LOCKED, RowState.SAVING -> colors.controlFill
    RowState.EMPTY -> Color.Transparent
    RowState.INCOMPLETE -> colors.amberFill
    RowState.SAVED -> colors.savedFill
    RowState.FAILED -> colors.errorFill
}

private fun RowState.foreground(colors: PendingBetColors): Color = when (this) {
    RowState.LOCKED, RowState.EMPTY, RowState.SAVING -> colors.secondaryText
    RowState.INCOMPLETE -> colors.amber
    RowState.SAVED -> colors.savedText
    RowState.FAILED -> colors.errorText
}

/** What the pill holds in one state: the ball in a circle as tall as the pill, or icon and text. */
@Composable
private fun PendingBetStatusContent(rowState: RowState, isPlaceholder: Boolean, height: Dp) {
    if (rowState == RowState.SAVING && !isPlaceholder) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(height)) {
            BallSpinner(modifier = Modifier.size(PendingBetStyle.spinnerSize))
        }
        return
    }

    val colors = pendingBetColors()
    val mask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(color = pulse.color, shape = CircleShape, highlight = pulse.highlight)
    } else {
        Modifier
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(height)
            .then(mask)
            .padding(horizontal = PendingBetStyle.pillHorizontalPadding),
    ) {
        val glyph = rowState.glyph
        if (glyph != null && !isPlaceholder) {
            Icon(
                painter = painterResource(glyph),
                contentDescription = null,
                tint = rowState.foreground(colors),
                modifier = Modifier.size(PendingBetStyle.pillGlyphSize * LocalDensity.current.fontScale.coerceAtLeast(1f)),
            )
        }
        Text(
            text = stringResource(rowState.statusText),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = rowState.foreground(colors),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@PreviewLightDark
@Composable
private fun PendingBetStatusPillPreview() {
    TycheTheme {
        Surface(color = pendingBetColors().cardFill) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(16.dp),
            ) {
                RowState.entries.forEach { state ->
                    PendingBetStatusPill(rowState = state, isPlaceholder = false, announcement = null)
                }
                PendingBetStatusPill(rowState = RowState.LOCKED, isPlaceholder = true, announcement = null)
            }
        }
    }
}
