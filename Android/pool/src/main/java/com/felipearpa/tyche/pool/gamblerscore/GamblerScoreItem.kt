package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun GamblerScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (poolGamblerScore.currentPosition != null) {
                Position(
                    position = poolGamblerScore.currentPosition,
                    isCurrentUser = isCurrentUser,
                    shimmerModifier = shimmerModifier,
                )
            } else {
                Box(modifier = Modifier.size(scoreSize))
            }

            Text(
                text = poolGamblerScore.gamblerUsername,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                ),
                modifier = shimmerModifier,
            )
        }

        Row {
            poolGamblerScore.score?.let { score ->
                Text(text = score.toString(), modifier = shimmerModifier)
            }

            poolGamblerScore.difference()?.let { difference ->
                Box(
                    modifier = Modifier.width(trendIndicatorSize),
                    contentAlignment = Alignment.Center,
                ) {
                    TrendIndicator(
                        difference = difference,
                        shimmerModifier = shimmerModifier,
                    )
                }
            }
        }
    }
}

@Composable
fun Position(
    position: Int,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(scoreSize)
            .clip(CircleShape)
            .background(color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .then(shimmerModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = position.toString(),
            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun GamblerScorePlaceholderItem(modifier: Modifier = Modifier) {
    GamblerScoreItem(
        poolGamblerScore = poolGamblerScorePlaceholderModel(),
        isCurrentUser = false,
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

private val scoreSize = 32.dp
private val trendIndicatorSize = 32.dp

@PreviewLightDark
@Composable
private fun NonCurrentUserGamblerScoreItemPreview() {
    TycheTheme {
        Surface {
            GamblerScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                isCurrentUser = false,
                modifier = Modifier.fillMaxWidth(),
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
                poolGamblerScore = PoolGamblerScoreModel(
                    poolId = "X".repeat(15),
                    poolName = "Tyche American Cup YYYY",
                    gamblerId = "X".repeat(15),
                    gamblerUsername = "user-tyche",
                    currentPosition = 1,
                    beforePosition = 2,
                    score = 10,
                ),
                isCurrentUser = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun GamblerScorePlaceholderItemPreview() {
    Surface {
        GamblerScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
    }
}
