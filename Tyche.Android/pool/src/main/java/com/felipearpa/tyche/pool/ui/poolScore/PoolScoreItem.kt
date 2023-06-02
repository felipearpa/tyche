package com.felipearpa.tyche.pool.ui.poolScore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.core.type.Ulid
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.ui.PoolGamblerScoreModel
import com.felipearpa.tyche.ui.ProgressIndicator

@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier) {
        val (dataView, progressView) = createRefs()

        Column(
            modifier = Modifier
                .constrainAs(dataView) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(progressView.start)
                    width = Dimension.fillToConstraints
                }
        ) {
            Text(text = poolGamblerScore.poolName, modifier = shimmerModifier)
            Position(
                currentPosition = poolGamblerScore.currentPosition,
                shimmerModifier = shimmerModifier
            )
        }

        Box(
            modifier = Modifier
                .width(32.dp)
                .constrainAs(progressView) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(dataView.end)
                    end.linkTo(parent.end)
                }, contentAlignment = Alignment.Center
        ) {
            ProgressIndicator(
                shimmerModifier = shimmerModifier,
                difference = poolGamblerScore.calculateDifference()
            )
        }
    }
}

@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    modifier: Modifier = Modifier
) {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScore,
        modifier = modifier,
        shimmerModifier = Modifier
    )
}

@Composable
private fun Position(currentPosition: Int?, shimmerModifier: Modifier = Modifier) {
    currentPosition?.let { nonNullableCurrentPosition ->
        Text(
            text = stringResource(R.string.position_label, nonNullableCurrentPosition),
            modifier = shimmerModifier,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PoolScoreItemPreview() {
    PoolScoreItem(
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
        modifier = Modifier.fillMaxWidth()
    )
}