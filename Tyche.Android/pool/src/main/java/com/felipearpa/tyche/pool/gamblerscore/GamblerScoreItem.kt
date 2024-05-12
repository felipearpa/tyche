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
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.ui.ProgressIndicator
import com.felipearpa.tyche.ui.preview.UIModePreview
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun GamblerScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            poolGamblerScore.currentPosition?.let { nonNullCurrentPosition ->
                Position(
                    position = nonNullCurrentPosition,
                    isActiveGambler = isLoggedIn,
                    shimmerModifier = shimmerModifier
                )
            }
            Text(text = poolGamblerScore.gamblerUsername, modifier = shimmerModifier)
        }

        Row {
            poolGamblerScore.score?.let { score ->
                Text(text = score.toString(), modifier = shimmerModifier)
            }

            poolGamblerScore.difference()?.let { nonNullDifference ->
                Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    ProgressIndicator(
                        difference = nonNullDifference,
                        shimmerModifier = shimmerModifier
                    )
                }
            }
        }
    }
}

@Composable
fun Position(
    position: Int,
    isActiveGambler: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color = if (isActiveGambler) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .then(shimmerModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            color = if (isActiveGambler) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@UIModePreview
@Composable
private fun NonLoggedInGamblerScoreItemPreview() {
    TycheTheme {
        Surface {
            GamblerScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                isLoggedIn = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@UIModePreview
@Composable
fun LoggedInGamblerScoreItemPreview() {
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
                    score = 10
                ),
                isLoggedIn = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}