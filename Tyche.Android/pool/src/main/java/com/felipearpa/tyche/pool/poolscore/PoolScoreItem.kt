package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.ui.progress.ProgressIndicator

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
            poolGamblerScore.currentPosition?.let { nonNullCurrentPosition ->
                Text(
                    text = stringResource(R.string.position_label, nonNullCurrentPosition),
                    modifier = shimmerModifier,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        poolGamblerScore.difference()?.let { nonNullDifference ->
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
                    difference = nonNullDifference
                )
            }
        }
    }
}

@Preview
@Composable
fun PoolScoreItemPreview() {
    Surface {
        PoolScoreItem(
            poolGamblerScore = poolGamblerScoreDummyModel(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}