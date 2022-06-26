package com.pipel.tyche.poolGambler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.pipel.core.type.Ulid
import com.pipel.tyche.poolGambler.view.PoolGamblerModel
import com.pipel.tyche.ui.ProgressIndicator
import com.pipel.tyche.ui.theme.onPrimaryLight
import com.pipel.tyche.ui.theme.primaryLight
import com.pipel.tyche.view.Progress

@Composable
private fun PoolGamblerItem(
    poolGambler: PoolGamblerModel,
    modifier: Modifier = Modifier,
    childModifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier) {
        val (dataView, progressView) = createRefs()

        Row(
            modifier = childModifier
                .constrainAs(dataView) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(progressView.start)
                    width = Dimension.fillToConstraints
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            poolGambler.progress.currentPosition?.let { currentPosition ->
                Position(position = currentPosition)
            }
            Gambler(gamblerEmail = poolGambler.gamblerEmail)
        }

        Row(
            modifier = childModifier.constrainAs(progressView) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(dataView.end)
                end.linkTo(parent.end)
            }, horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            poolGambler.score?.let { score ->
                Score(score = score)
            }
            ProgressIndicator(difference = poolGambler.progress.calculateDifference())
        }
    }
}

@Composable
fun Position(position: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = MaterialTheme.colors.primaryLight)
    ) {
        Text(
            text = position.toString(),
            modifier = Modifier.padding(4.dp),
            color = MaterialTheme.colors.onPrimaryLight
        )
    }
}

@Composable
fun Gambler(gamblerEmail: String) {
    Text(text = gamblerEmail)
}

@Composable
fun Score(score: Int) {
    Text(text = score.toString())
}

@Composable
fun PoolGamblerItem(poolGambler: PoolGamblerModel, modifier: Modifier = Modifier) {
    PoolGamblerItem(poolGambler = poolGambler, modifier = modifier, childModifier = Modifier)
}

@Composable
fun FakePoolGamblerItem(modifier: Modifier = Modifier) {
    PoolGamblerItem(
        poolGambler = PoolGamblerModel(
            poolId = Ulid.randomUlid().toString(),
            gamblerId = Ulid.randomUlid().toString(),
            gamblerEmail = "email@tyche.com",
            score = 10,
            progress = Progress(currentPosition = 1, beforePosition = 2)
        ),
        modifier = modifier,
        childModifier = Modifier.placeholder(
            visible = true,
            highlight = PlaceholderHighlight.shimmer()
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerItemPreview() {
    PoolGamblerItem(
        poolGambler = poolsGamblersModelsForPreview().iterator().next(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolGamblerItemFakePreview() {
    FakePoolGamblerItem(modifier = Modifier.fillMaxWidth())
}