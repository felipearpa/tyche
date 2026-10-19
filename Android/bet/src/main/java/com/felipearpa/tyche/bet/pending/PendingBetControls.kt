package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

/** A `+` or `−` button beside a team's score box: a 52 × 44 dp shape in a 48-dp-tall touch target. */
@Composable
internal fun BetScoreStepper(
    isIncrease: Boolean,
    teamName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = pendingBetColors()
    val label = stringResource(
        if (isIncrease) R.string.bet_increase_score_accessibility else R.string.bet_decrease_score_accessibility,
        teamName,
    )
    PendingBetPressable(
        onClick = onClick,
        label = label,
        shape = RoundedCornerShape(PendingBetStyle.scoreBoxCornerRadius),
        modifier = modifier,
        shapeModifier = Modifier
            .size(width = PendingBetStyle.stepperWidth, height = PendingBetStyle.stepperHeight)
            .background(colors.stepperFill),
    ) {
        Icon(
            painter = painterResource(if (isIncrease) SharedR.drawable.add else SharedR.drawable.remove),
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(PendingBetStyle.stepperGlyphSize),
        )
    }
}

/**
 * A custom-drawn button: the touch target, and the node TalkBack and the Accessibility Scanner
 * measure, is at least 48 dp tall and wide around a smaller [shape], which shows the ripple.
 */
@Composable
private fun PendingBetPressable(
    onClick: () -> Unit,
    label: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    shapeModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = MINIMUM_TARGET, minHeight = MINIMUM_TARGET)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(shape)
                .then(shapeModifier)
                .indication(interactionSource, ripple()),
        ) {
            content()
        }
    }
}

private val MINIMUM_TARGET = 48.dp

/** The failed save's banner: the kept-draft message and Retry. */
@Composable
internal fun BetSaveFailureBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = pendingBetColors()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.errorFill, RoundedCornerShape(12.dp))
            .padding(start = 14.dp, end = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.bet_save_failure_text),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.errorText,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
        PendingBetPressable(
            onClick = onRetry,
            label = null,
            shape = CircleShape,
            shapeModifier = Modifier
                .height(PendingBetStyle.chipHeight)
                .background(colors.retryFill)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(SharedR.string.retry_action),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onRetry,
                maxLines = 1,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PendingBetControlsPreview() {
    TycheTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(pendingBetColors().cardFill)
                    .padding(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BetScoreStepper(isIncrease = true, teamName = "Colombia", onClick = {})
                    BetScoreStepper(isIncrease = false, teamName = "Colombia", onClick = {})
                }
                BetSaveFailureBanner(onRetry = {})
            }
        }
    }
}
