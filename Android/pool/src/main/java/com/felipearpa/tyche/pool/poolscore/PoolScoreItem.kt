package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.PositionIndicator
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.movement
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreTrendStateDummyModels
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutPositionDummyModel
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.accessibilityText
import com.felipearpa.tyche.ui.hasLabel
import com.felipearpa.tyche.ui.labelWordWidth
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
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
        val spacing = LocalBoxSpacing.current
        val rowModifier = Modifier
            .fillMaxWidth()
            .padding(all = spacing.medium)
            // The row owns its announcement, so the rank tile's bare "4" and the
            // trend indicator's bare "1" never reach TalkBack on their own. Clearing
            // also drops the invite button's node; `PoolScoreList` re-exposes invite
            // as a custom action on the clickable row that wraps this one.
            .clearAndSetSemantics {
                if (!isPlaceholder) contentDescription = accessibilityDescription
            }
        val rail = @Composable { RankRail(poolGamblerScore = poolGamblerScore, isPlaceholder = isPlaceholder) }
        val identity = @Composable { identityModifier: Modifier ->
            Identity(poolGamblerScore = poolGamblerScore, leafMask = leafMask, modifier = identityModifier)
        }
        // A placeholder keeps the button's footprint but cannot start an invitation.
        val invite = @Composable {
            IconButton(onClick = onJoin, enabled = !isPlaceholder) {
                Icon(
                    painter = painterResource(SharedR.drawable.person_add),
                    contentDescription = null,
                    modifier = leafMask,
                )
            }
        }

        if (LocalDensity.current.fontScale >= REFLOW_FONT_SCALE) {
            // Three columns would squeeze the pool name until words break, so the rail and the
            // invite control share the first line and the name and counts get the full width.
            Column(modifier = rowModifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    rail()
                    Spacer(modifier = Modifier.weight(1f))
                    invite()
                }
                identity(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rail()
                identity(Modifier.weight(1f))
                invite()
            }
        }
    }
}

/**
 * The rank tile with the movement beneath it. A qualifier or state label gets a definite width,
 * scaled with the font, so it wraps on word boundaries beneath the tile, never narrower than its
 * widest word.
 */
@Composable
private fun RankRail(poolGamblerScore: PoolGamblerScoreModel, isPlaceholder: Boolean) {
    val movement = poolGamblerScore.movement()
    val trendTextStyle = MaterialTheme.typography.labelSmall
    val railWidth = if (movement.hasLabel) {
        val labelWidth = with(LocalDensity.current) { RAIL_LABEL_WIDTH.toDp() }
        Modifier.width(maxOf(labelWidth, movement.labelWordWidth(trendTextStyle)))
    } else {
        Modifier
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
        modifier = railWidth,
    ) {
        poolGamblerScore.position?.let {
            PositionIndicator(
                position = it,
                shouldUsePrimaryColor = false,
                isPlaceholder = isPlaceholder,
            )
        }
        TrendIndicator(
            movement = movement,
            textStyle = trendTextStyle,
            isPlaceholder = isPlaceholder,
        )
    }
}

/** The pool name, then points and member count; the count moves below whole when they don't fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Identity(poolGamblerScore: PoolGamblerScoreModel, leafMask: Modifier, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = poolGamblerScore.poolName,
            modifier = leafMask,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
            itemVerticalAlignment = Alignment.CenterVertically,
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
    return listOfNotNull(
        poolGamblerScore.poolName,
        rank,
        score,
        gamblerCount,
        poolGamblerScore.movement().accessibilityText(),
    ).joinToString(separator = ", ")
}

/** Room for a partial group's qualifier or a pending or unavailable label beneath the tile. */
private val RAIL_LABEL_WIDTH = 64.sp

/** At or above this font scale the row reflows instead of squeezing the pool name. */
private const val REFLOW_FONT_SCALE = 1.5f

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

@PreviewLightDark
@Preview(name = "Trend states, large text", fontScale = 2f, widthDp = 320)
@Preview(name = "Trend states, Spanish (Spain)", locale = "es-rES")
@Composable
private fun PoolScoreItemTrendStatesPreview() {
    TycheTheme {
        Surface {
            Column {
                poolGamblerScoreTrendStateDummyModels().forEach { score ->
                    PoolScoreItem(poolGamblerScore = score, onJoin = {}, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
