package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.PositionIndicator
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutPositionDummyModel
import com.felipearpa.tyche.pool.rank
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import kotlin.math.abs
import com.felipearpa.tyche.ui.R as SharedR

/**
 * One My pools row. Loading rows render this same component from
 * `poolGamblerScorePlaceholderModel()` with [isPlaceholder] set; the row then masks each content
 * leaf (rank digits, trend, pool name, points, member count, invite glyph) with the shared
 * [LocalLoadingPlaceholderPulse], disables the invite button, and exposes nothing to TalkBack.
 * The rank tile keeps its container color. Callers pass no effect.
 */
@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    // Applied to each text and glyph leaf separately, never to the whole row.
    val leafMask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(
            color = pulse.color,
            shape = pulse.shape,
            highlight = pulse.highlight,
        )
    } else {
        Modifier
    }
    val accessibilityDescription = poolScoreAccessibilityDescription(poolGamblerScore)

    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium)
                // The row owns its announcement, so the rank tile's bare "4" and the
                // trend indicator's bare "1" never reach TalkBack on their own. Clearing
                // also drops the invite button's node; `PoolScoreList` re-exposes invite
                // as a custom action on the clickable row that wraps this one.
                .clearAndSetSemantics {
                    if (!isPlaceholder) contentDescription = accessibilityDescription
                },
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            ) {
                poolGamblerScore.position?.let {
                    PositionIndicator(
                        position = it,
                        shouldUsePrimaryColor = false,
                        isPlaceholder = isPlaceholder,
                    )
                }
                poolGamblerScore.rank()?.let {
                    TrendIndicator(
                        rank = it,
                        textStyle = MaterialTheme.typography.labelSmall,
                        isPlaceholder = isPlaceholder,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poolGamblerScore.poolName,
                    modifier = leafMask,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    poolGamblerScore.score?.let {
                        Text(
                            text = stringResource(id = R.string.points_text, it),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = leafMask,
                        )
                    }

                    poolGamblerScore.gamblerCount?.let {
                        Text(
                            text = pluralStringResource(R.plurals.gamblers_text, it, it),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = leafMask,
                        )
                    }
                }
            }

            // A placeholder keeps the button's footprint but cannot start an invitation.
            IconButton(
                onClick = onJoin,
                enabled = !isPlaceholder,
            ) {
                Icon(
                    painter = painterResource(SharedR.drawable.person_add),
                    contentDescription = null,
                    modifier = leafMask,
                )
            }
        }
    }
}

/**
 * The row's single announcement, composed from the model rather than from the abbreviated
 * visible copy. Unlike the leaderboard row it leads with the pool name: these rows are not
 * a ranking, so the name is what distinguishes one row from the next.
 */
@Composable
private fun poolScoreAccessibilityDescription(
    poolGamblerScore: PoolGamblerScoreModel,
): String {
    val rank = poolGamblerScore.position?.let {
        stringResource(R.string.leaderboard_rank_accessibility, it)
    } ?: stringResource(R.string.leaderboard_rank_missing_accessibility)
    val score = poolGamblerScore.score?.let {
        pluralStringResource(R.plurals.leaderboard_points_accessibility, it, it)
    } ?: stringResource(R.string.leaderboard_score_missing_accessibility)
    // The visible member count already spells the word out, so the announcement reuses
    // its resource rather than duplicating it under an accessibility key — including its
    // plural agreement, since the row above renders the same one.
    val gamblerCount = poolGamblerScore.gamblerCount?.let {
        pluralStringResource(R.plurals.gamblers_text, it, it)
    }
    val movement = poolGamblerScore.rank()?.let { difference ->
        when {
            difference > 0 -> pluralStringResource(
                R.plurals.leaderboard_movement_up_accessibility,
                abs(difference),
                abs(difference),
            )

            difference < 0 -> pluralStringResource(
                R.plurals.leaderboard_movement_down_accessibility,
                abs(difference),
                abs(difference),
            )

            else -> stringResource(R.string.leaderboard_movement_unchanged_accessibility)
        }
    }

    return listOfNotNull(
        poolGamblerScore.poolName,
        rank,
        score,
        gamblerCount,
        movement,
    ).joinToString(separator = ", ")
}

@PreviewLightDark
@Composable
private fun PoolScoreItemPreview() {
    TycheTheme {
        Surface {
            PoolScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                onJoin = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolScoreItemPreviewWithoutScore() {
    TycheTheme {
        Surface {
            PoolScoreItem(
                poolGamblerScore = poolGamblerScoreWithoutPositionDummyModel(),
                onJoin = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolScoreItemLongPoolNamePreview() {
    TycheTheme {
        Surface {
            PoolScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel().copy(
                    poolName = "This is a very long pool name to test how it is displayed in the list",
                ),
                onJoin = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolScorePlaceholderItemPreview() {
    TycheTheme {
        Surface {
            Column {
                PoolScoreItem(
                    poolGamblerScore = poolGamblerScoreDummyModel(),
                    onJoin = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                PoolScoreItem(
                    poolGamblerScore = poolGamblerScorePlaceholderModel(),
                    onJoin = {},
                    modifier = Modifier.fillMaxWidth(),
                    isPlaceholder = true,
                )
            }
        }
    }
}
