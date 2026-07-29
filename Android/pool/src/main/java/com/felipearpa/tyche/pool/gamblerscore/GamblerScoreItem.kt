package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.account.AccountAvatarFallback
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.PositionIndicator
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModelWithoutPosition
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.rank
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlin.math.abs

@Composable
fun GamblerScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    if (isPlaceholder) {
        GamblerScorePlaceholderContent(modifier = modifier)
        return
    }

    val extendedColors = LocalExtendedColorScheme.current
    val rowBackground = if (isCurrentUser) {
        extendedColors.currentUserContainer
    } else {
        Color.Transparent
    }
    val rowForeground = if (isCurrentUser) {
        extendedColors.onCurrentUserContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val rankBackground = if (isCurrentUser) {
        extendedColors.currentUser
            .copy(alpha = CURRENT_USER_TILE_OVERLAY_OPACITY)
            .compositeOver(extendedColors.currentUserContainer)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val rankForeground = if (isCurrentUser) {
        extendedColors.onCurrentUserContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val accessibilityDescription = gamblerScoreAccessibilityDescription(
        poolGamblerScore = poolGamblerScore,
        isCurrentUser = isCurrentUser,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MINIMUM_HEIGHT)
            .background(rowBackground)
            .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING)
            .clearAndSetSemantics {
                contentDescription = accessibilityDescription
            },
        horizontalArrangement = Arrangement.spacedBy(IDENTITY_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(RANK_TILE_SIZE),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PositionIndicator(
                position = poolGamblerScore.position,
                shouldUsePrimaryColor = false,
                size = RANK_TILE_SIZE,
                shape = RoundedCornerShape(RANK_CORNER_RADIUS),
                containerColor = rankBackground,
                contentColor = rankForeground,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
            )

            Spacer(modifier = Modifier.height(RANK_SPACING))

            Box(
                modifier = Modifier.height(MOVEMENT_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                poolGamblerScore.rank()?.let { difference ->
                    TrendIndicator(
                        rank = difference,
                        textStyle = MaterialTheme.typography.labelSmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
            }
        }

        AccountAvatar(
            accountId = poolGamblerScore.gamblerId,
            fallback = AccountAvatarFallback(
                identity = poolGamblerScore.gamblerUsername,
                colorKey = poolGamblerScore.gamblerUsername,
                backgroundColor = if (isCurrentUser) extendedColors.currentUser else null,
                foregroundColor = if (isCurrentUser) extendedColors.onCurrentUser else null,
            ),
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(IDENTITY_TEXT_SPACING),
        ) {
            Text(
                text = poolGamblerScore.gamblerUsername,
                color = rowForeground,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isCurrentUser) {
                Text(
                    text = stringResource(R.string.leaderboard_you),
                    color = rowForeground,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Text(
            text = poolGamblerScore.score?.toString() ?: "—",
            color = rowForeground,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = SCORE_MINIMUM_WIDTH),
        )
    }
}

@Composable
fun GamblerScorePlaceholderItem(modifier: Modifier = Modifier) {
    GamblerScoreItem(
        poolGamblerScore = poolGamblerScorePlaceholderModel(),
        isCurrentUser = false,
        modifier = modifier,
        isPlaceholder = true,
    )
}

@Composable
private fun GamblerScorePlaceholderContent(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MINIMUM_HEIGHT)
            .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(IDENTITY_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(RANK_TILE_SIZE),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(RANK_TILE_SIZE)
                    .clip(RoundedCornerShape(RANK_CORNER_RADIUS))
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(RANK_SPACING))
            Box(
                modifier = Modifier
                    .size(MOVEMENT_PLACEHOLDER_WIDTH, MOVEMENT_PLACEHOLDER_HEIGHT)
                    .clip(CircleShape)
                    .shimmer(),
            )
        }

        Box(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .shimmer(),
        )

        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .widthIn(max = USERNAME_PLACEHOLDER_WIDTH)
                    .fillMaxWidth()
                    .height(USERNAME_PLACEHOLDER_HEIGHT)
                    .clip(RoundedCornerShape(PLACEHOLDER_CORNER_RADIUS))
                    .shimmer(),
            )
        }

        Box(
            modifier = Modifier
                .size(SCORE_PLACEHOLDER_WIDTH, SCORE_PLACEHOLDER_HEIGHT)
                .clip(RoundedCornerShape(PLACEHOLDER_CORNER_RADIUS))
                .shimmer(),
        )
    }
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
        stringResource(R.string.leaderboard_points_accessibility, it)
    } ?: stringResource(R.string.leaderboard_score_missing_accessibility)
    val movement = poolGamblerScore.rank()?.let { difference ->
        when {
            difference > 0 -> stringResource(
                R.string.leaderboard_movement_up_accessibility,
                abs(difference),
            )
            difference < 0 -> stringResource(
                R.string.leaderboard_movement_down_accessibility,
                abs(difference),
            )
            else -> stringResource(R.string.leaderboard_movement_unchanged_accessibility)
        }
    }

    return listOfNotNull(
        rank,
        poolGamblerScore.gamblerUsername,
        stringResource(R.string.leaderboard_you).takeIf { isCurrentUser },
        score,
        movement,
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
private val SCORE_MINIMUM_WIDTH = 52.dp
private val HORIZONTAL_PADDING = 16.dp
private val VERTICAL_PADDING = 10.dp
private const val CURRENT_USER_TILE_OVERLAY_OPACITY = 0.14f
private val MOVEMENT_PLACEHOLDER_WIDTH = 18.dp
private val MOVEMENT_PLACEHOLDER_HEIGHT = 6.dp
private val USERNAME_PLACEHOLDER_WIDTH = 144.dp
private val USERNAME_PLACEHOLDER_HEIGHT = 16.dp
private val SCORE_PLACEHOLDER_WIDTH = 48.dp
private val SCORE_PLACEHOLDER_HEIGHT = 24.dp
private val PLACEHOLDER_CORNER_RADIUS = 4.dp

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

@Preview(showBackground = true)
@Composable
private fun GamblerScorePlaceholderItemPreview() {
    TycheTheme {
        GamblerScorePlaceholderItem()
    }
}
