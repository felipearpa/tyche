package com.felipearpa.tyche.pool.ui.gamblerScore

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.core.type.Ulid
import com.felipearpa.tyche.pool.ui.PoolGamblerScoreModel
import com.felipearpa.tyche.ui.ProgressIndicator

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

            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                ProgressIndicator(
                    difference = poolGamblerScore.calculateDifference(),
                    shimmerModifier = shimmerModifier
                )
            }
        }
    }
}

@Composable
fun GamblerScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier
) {
    GamblerScoreItem(
        poolGamblerScore = poolGamblerScore,
        modifier = modifier,
        isLoggedIn = isLoggedIn,
        shimmerModifier = Modifier
    )
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
            .background(color = if (isActiveGambler) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            .then(shimmerModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = position.toString(),
            modifier = Modifier.padding(4.dp),
            color = if (isActiveGambler) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NonLoggedInGamblerScoreItemPreview() {
    GamblerScoreItem(
        poolGamblerScore = PoolGamblerScoreModel(
            poolId = Ulid.randomUlid().value,
            poolLayoutId = Ulid.randomUlid().value,
            poolName = "Tyche American Cup YYYY",
            gamblerId = Ulid.randomUlid().value,
            gamblerUsername = "user-tyche",
            currentPosition = 1,
            beforePosition = 2,
            score = 10
        ),
        isLoggedIn = false,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun LoggedInGamblerScoreItemPreview() {
    GamblerScoreItem(
        poolGamblerScore = PoolGamblerScoreModel(
            poolId = Ulid.randomUlid().value,
            poolLayoutId = Ulid.randomUlid().value,
            poolName = "Tyche American Cup YYYY",
            gamblerId = Ulid.randomUlid().value,
            gamblerUsername = "user-tyche",
            currentPosition = 1,
            beforePosition = 2,
            score = 10
        ),
        isLoggedIn = true,
        modifier = Modifier.fillMaxWidth()
    )
}