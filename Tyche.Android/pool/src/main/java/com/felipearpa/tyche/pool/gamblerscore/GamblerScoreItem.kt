package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.ui.progress.ProgressIndicator

@Composable
fun GamblerScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier) {
        val (dataView, progressView) = createRefs()

        Row(
            modifier = Modifier.constrainAs(dataView) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(progressView.start)
                width = Dimension.fillToConstraints
            },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            poolGamblerScore.currentPosition?.let { currentPosition ->
                Position(
                    position = currentPosition,
                    isActiveGambler = isLoggedIn,
                    shimmerModifier = shimmerModifier
                )
            }
            Text(text = poolGamblerScore.gamblerUsername, modifier = shimmerModifier)
        }

        Row(
            modifier = Modifier.constrainAs(progressView) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(dataView.end)
                end.linkTo(parent.end)
            },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    shimmerModifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color = if (isActiveGambler) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .then(shimmerModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            modifier = Modifier.padding(4.dp),
            color = if (isActiveGambler) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Preview
@Composable
fun NonLoggedInGamblerScoreItemPreview() {
    MaterialTheme {
        Surface {
            GamblerScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                isLoggedIn = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoggedInGamblerScoreItemPreview() {
    MaterialTheme {
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