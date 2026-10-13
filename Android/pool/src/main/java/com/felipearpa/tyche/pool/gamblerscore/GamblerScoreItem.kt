package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.account.AccountAvatarFallback
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.PositionIndicator
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.movement
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModelWithoutPosition
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreTrendStateDummyModels
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.accessibilityText
import com.felipearpa.tyche.ui.hasLabel
import com.felipearpa.tyche.ui.labelWordWidth
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * One leaderboard row. Loading rows render this same component from
 * `poolGamblerScorePlaceholderModel()` with [isPlaceholder] set; the row then masks each content
 * leaf (rank digits, trend, avatar, username, score) with the shared
 * [LocalLoadingPlaceholderPulse], requests no avatar, and exposes nothing to TalkBack. The rank
 * tile keeps its container color. Callers pass no effect.
 *
 * At ordinary font scales the username stays on one line, ellipsized before the score. At large
 * font scales one line cannot hold the rail, avatar, username, and score without clipping or
 * truncating them, so the row reflows: the rail, avatar, and score share the first line (the score
 * moves below when even that does not fit), and the username wraps across the full width beneath.
 */
@Composable
fun GamblerScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val extendedColors = LocalExtendedColorScheme.current
    val rowBackground = if (isCurrentUser && !isPlaceholder) {
        extendedColors.currentUserContainer
    } else {
        Color.Transparent
    }
    val parts = GamblerScoreParts(
        poolGamblerScore = poolGamblerScore,
        isCurrentUser = isCurrentUser,
        isPlaceholder = isPlaceholder,
    )
    val accessibilityDescription = gamblerScoreAccessibilityDescription(
        poolGamblerScore = poolGamblerScore,
        isCurrentUser = isCurrentUser,
    )
    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = ROW_MINIMUM_HEIGHT)
        .background(rowBackground)
        .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING)
        .clearAndSetSemantics {
            if (!isPlaceholder) contentDescription = accessibilityDescription
        }

    if (LocalDensity.current.fontScale >= REFLOW_FONT_SCALE) {
        ReflowedContent(parts = parts, modifier = rowModifier)
    } else {
        InlineContent(parts = parts, modifier = rowModifier)
    }
}

/** The pieces both layouts arrange. */
private class GamblerScoreParts(
    val poolGamblerScore: PoolGamblerScoreModel,
    val isCurrentUser: Boolean,
    val isPlaceholder: Boolean,
)

@Composable
private fun InlineContent(parts: GamblerScoreParts, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(IDENTITY_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankRail(parts = parts)
        Avatar(parts = parts)
        Identity(parts = parts, isSingleLine = true, modifier = Modifier.weight(1f))
        Score(parts = parts, modifier = Modifier.widthIn(min = SCORE_MINIMUM_WIDTH))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReflowedContent(parts: GamblerScoreParts, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(REFLOWED_SPACING)) {
        FlowRow(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(REFLOWED_SPACING),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(IDENTITY_SPACING)) {
                RankRail(parts = parts)
                Avatar(parts = parts)
            }
            Score(parts = parts)
        }
        Identity(parts = parts, isSingleLine = false, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The rank tile with the movement beneath it. A movement with a qualifier or state label gets a
 * wider definite width, scaled with the font and never narrower than the label's widest word, so
 * the label wraps on word boundaries. A plain
 * movement is as wide as the tile, or as its symbol and number when large text makes them wider,
 * so they never clip.
 */
@Composable
private fun RankRail(parts: GamblerScoreParts) {
    val extendedColors = LocalExtendedColorScheme.current
    val movement = parts.poolGamblerScore.movement()
    val rankBackground = if (parts.isCurrentUser) {
        extendedColors.currentUser
            .copy(alpha = CURRENT_USER_TILE_OVERLAY_OPACITY)
            .compositeOver(extendedColors.currentUserContainer)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val rankForeground = if (parts.isCurrentUser) {
        extendedColors.onCurrentUserContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val trendTextStyle = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    val railWidth = if (movement.hasLabel) {
        val labelWidth = with(LocalDensity.current) { RAIL_LABEL_WIDTH.toDp() }
        Modifier.width(maxOf(labelWidth, movement.labelWordWidth(trendTextStyle)))
    } else {
        Modifier.widthIn(min = RANK_TILE_SIZE)
    }

    Column(
        modifier = railWidth,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RANK_SPACING),
    ) {
        PositionIndicator(
            position = parts.poolGamblerScore.position,
            shouldUsePrimaryColor = false,
            size = RANK_TILE_SIZE,
            shape = RoundedCornerShape(RANK_CORNER_RADIUS),
            containerColor = rankBackground,
            contentColor = rankForeground,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            isPlaceholder = parts.isPlaceholder,
        )
        TrendIndicator(
            movement = movement,
            textStyle = trendTextStyle,
            isPlaceholder = parts.isPlaceholder,
            modifier = Modifier.heightIn(min = MOVEMENT_HEIGHT),
        )
    }
}

@Composable
private fun Avatar(parts: GamblerScoreParts) {
    val extendedColors = LocalExtendedColorScheme.current
    val pulse = LocalLoadingPlaceholderPulse.current
    AccountAvatar(
        accountId = parts.poolGamblerScore.gamblerId,
        fallback = AccountAvatarFallback(
            identity = parts.poolGamblerScore.gamblerUsername,
            colorKey = parts.poolGamblerScore.gamblerUsername,
            backgroundColor = if (parts.isCurrentUser) extendedColors.currentUser else null,
            foregroundColor = if (parts.isCurrentUser) extendedColors.onCurrentUser else null,
        ),
        modifier = Modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .then(
                if (parts.isPlaceholder) {
                    Modifier.placeholder(
                        color = pulse.color,
                        shape = CircleShape,
                        highlight = pulse.highlight,
                    )
                } else {
                    Modifier
                },
            ),
        isPlaceholder = parts.isPlaceholder,
    )
}

@Composable
private fun Identity(parts: GamblerScoreParts, isSingleLine: Boolean, modifier: Modifier) {
    val foreground = rowForeground(parts.isCurrentUser)
    val leafMask = leafMask(parts.isPlaceholder)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(IDENTITY_TEXT_SPACING),
    ) {
        Text(
            text = parts.poolGamblerScore.gamblerUsername,
            color = foreground,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = if (isSingleLine) 1 else Int.MAX_VALUE,
            overflow = if (isSingleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = leafMask,
        )
        if (parts.isCurrentUser) {
            Text(
                text = stringResource(R.string.leaderboard_you),
                color = foreground,
                style = MaterialTheme.typography.bodySmall,
                modifier = leafMask,
            )
        }
    }
}

@Composable
private fun Score(parts: GamblerScoreParts, modifier: Modifier = Modifier) {
    Text(
        text = parts.poolGamblerScore.score?.toString() ?: "—",
        color = rowForeground(parts.isCurrentUser),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
        ),
        maxLines = 1,
        textAlign = TextAlign.End,
        modifier = modifier.then(leafMask(parts.isPlaceholder)),
    )
}

@Composable
private fun rowForeground(isCurrentUser: Boolean): Color =
    if (isCurrentUser) LocalExtendedColorScheme.current.onCurrentUserContainer else MaterialTheme.colorScheme.onSurface

@Composable
private fun leafMask(isPlaceholder: Boolean): Modifier {
    if (!isPlaceholder) return Modifier
    val pulse = LocalLoadingPlaceholderPulse.current
    return Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
}

@Composable
private fun gamblerScoreAccessibilityDescription(
    poolGamblerScore: PoolGamblerScoreModel,
    isCurrentUser: Boolean,
): String {
    val rank = poolGamblerScore.position?.let {
        stringResource(R.string.leaderboard_rank_accessibility, it)
    } ?: stringResource(R.string.leaderboard_rank_missing_accessibility)
    val score = poolGamblerScore.score?.let {
        pluralStringResource(R.plurals.leaderboard_points_accessibility, it, it)
    } ?: stringResource(R.string.leaderboard_score_missing_accessibility)

    return listOfNotNull(
        rank,
        poolGamblerScore.gamblerUsername,
        stringResource(R.string.leaderboard_you).takeIf { isCurrentUser },
        score,
        poolGamblerScore.movement().accessibilityText(),
    ).joinToString(separator = ", ")
}

private val ROW_MINIMUM_HEIGHT = 82.dp
private val RANK_TILE_SIZE = 44.dp
private val RANK_CORNER_RADIUS = 10.dp
private val AVATAR_SIZE = 40.dp
private val IDENTITY_SPACING = 12.dp
private val IDENTITY_TEXT_SPACING = 2.dp
private val RANK_SPACING = 2.dp
private val MOVEMENT_HEIGHT = 16.dp

/** Room for a partial group's qualifier or a pending or unavailable label beneath the tile. */
private val RAIL_LABEL_WIDTH = 64.sp
private val REFLOWED_SPACING = 8.dp

/** At or above this font scale the row reflows instead of ellipsizing the username. */
private const val REFLOW_FONT_SCALE = 1.5f
private val SCORE_MINIMUM_WIDTH = 52.dp
private val HORIZONTAL_PADDING = 16.dp
private val VERTICAL_PADDING = 10.dp
private const val CURRENT_USER_TILE_OVERLAY_OPACITY = 0.14f

@PreviewLightDark
@Composable
private fun NonCurrentUserGamblerScoreItemPreview() {
    TycheTheme {
        Surface {
            GamblerScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                isCurrentUser = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CurrentUserGamblerScoreItemPreview() {
    TycheTheme {
        Surface {
            GamblerScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                isCurrentUser = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GamblerScoreItemWithoutPositionPreview() {
    TycheTheme {
        GamblerScoreItem(
            poolGamblerScore = poolGamblerScoreDummyModelWithoutPosition(),
            isCurrentUser = false,
        )
    }
}

@PreviewLightDark
@Composable
private fun GamblerScoreItemGeometryPreview() {
    TycheTheme {
        Surface {
            Column {
                GamblerScoreItem(
                    poolGamblerScore = poolGamblerScoreDummyModel(),
                    isCurrentUser = false,
                )
                GamblerScoreItem(
                    poolGamblerScore = poolGamblerScorePlaceholderModel(),
                    isCurrentUser = false,
                    isPlaceholder = true,
                )
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Trend states, large text", fontScale = 2f, widthDp = 320)
@Preview(name = "Trend states, Spanish", locale = "es-rCO")
@Composable
private fun GamblerScoreItemTrendStatesPreview() {
    TycheTheme {
        Surface {
            Column {
                poolGamblerScoreTrendStateDummyModels().forEach { score ->
                    GamblerScoreItem(
                        poolGamblerScore = score.copy(
                            gamblerUsername = if (score.gamblerId == "trend-up") {
                                "A very long username that cannot fit on one line"
                            } else {
                                score.gamblerUsername
                            },
                        ),
                        isCurrentUser = score.gamblerId == "trend-partial",
                    )
                }
            }
        }
    }
}
