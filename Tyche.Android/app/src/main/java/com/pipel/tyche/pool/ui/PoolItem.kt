package com.pipel.tyche.pool.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.pipel.core.type.Ulid
import com.pipel.tyche.R
import com.pipel.tyche.pool.view.PoolModel
import com.pipel.tyche.ui.ProgressIndicator
import com.pipel.tyche.ui.theme.TycheTheme
import com.pipel.tyche.ui.theme.onPrimaryLight

@Composable
private fun PoolItem(
    pool: PoolModel,
    modifier: Modifier = Modifier,
    childModifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier) {
        val (dataView, progressView) = createRefs()

        Column(
            modifier = childModifier
                .constrainAs(dataView) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(progressView.start)
                    width = Dimension.fillToConstraints
                }
        ) {
            Text(text = pool.poolName)
            Position(currentPosition = pool.currentPosition)
        }

        ProgressIndicator(
            modifier = childModifier.constrainAs(progressView) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(dataView.end)
                end.linkTo(parent.end)
            },
            difference = pool.calculateDifference()
        )
    }
}

@Composable
fun PoolItem(pool: PoolModel, modifier: Modifier = Modifier) {
    PoolItem(pool = pool, modifier = modifier, childModifier = Modifier)
}

@Composable
fun FakePoolItem(modifier: Modifier = Modifier) {
    PoolItem(
        pool = PoolModel(
            poolId = Ulid.randomUlid().toString(),
            poolLayoutId = Ulid.randomUlid().toString(),
            poolName = "Copa América 2022 UTP",
            currentPosition = 1,
            beforePosition = 2
        ),
        modifier = modifier,
        childModifier = Modifier.placeholder(
            visible = true,
            highlight = PlaceholderHighlight.shimmer()
        )
    )
}

@Composable
private fun Position(currentPosition: Int?) {
    currentPosition?.let {
        Text(
            text = stringResource(R.string.position, it),
            color = MaterialTheme.colors.onPrimaryLight
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolItemPreview() {
    TycheTheme {
        PoolItem(
            pool = poolsModelsForPreview().iterator().next(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FakePreview() {
    TycheTheme {
        FakePoolItem(modifier = Modifier.fillMaxWidth())
    }
}