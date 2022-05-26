package com.pipel.tyche.pool.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.pipel.core.empty
import com.pipel.core.type.Ulid
import com.pipel.tyche.R
import com.pipel.tyche.pool.view.PoolModel
import com.pipel.tyche.ui.theme.*
import kotlin.math.abs

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

@Composable
private fun StableProgressIndicator(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier.size(24.dp, 24.dp),
        painter = painterResource(id = R.drawable.ic_baseline_remove_24),
        contentDescription = String.empty(),
        tint = MaterialTheme.colors.stableIndicator
    )
}

@Composable
private fun UpProgressIndicator(modifier: Modifier = Modifier, progress: Int) {
    Row(modifier = modifier) {
        Icon(
            modifier = Modifier.size(24.dp, 24.dp),
            painter = painterResource(id = R.drawable.ic_baseline_arrow_upward_24),
            contentDescription = String.empty(),
            tint = MaterialTheme.colors.upIndicator
        )
        Text(
            text = abs(progress).toString(),
            fontSize = 12.sp,
            color = MaterialTheme.colors.upIndicator
        )
    }
}

@Composable
private fun DownProgressIndicator(modifier: Modifier = Modifier, progress: Int) {
    Row(modifier = modifier) {
        Icon(
            modifier = Modifier.size(24.dp, 24.dp),
            painter = painterResource(id = R.drawable.ic_baseline_arrow_downward_24),
            contentDescription = String.empty(),
            tint = MaterialTheme.colors.downIndicator
        )
        Text(
            text = abs(progress).toString(),
            fontSize = 12.sp,
            color = MaterialTheme.colors.downIndicator
        )
    }
}

@Composable
private fun ProgressIndicator(
    modifier: Modifier = Modifier,
    difference: Int?
) {
    difference?.let {
        if (it == 0) {
            StableProgressIndicator(modifier = modifier)
        } else if (it > 0) {
            UpProgressIndicator(modifier = modifier, progress = it)
        } else {
            DownProgressIndicator(modifier = modifier, progress = it)
        }
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
private fun StableProgressIndicatorPreview() {
    TycheTheme {
        StableProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun UpProgressIndicatorPreview() {
    TycheTheme {
        UpProgressIndicator(progress = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun DownProgressIndicatorPreview() {
    TycheTheme {
        DownProgressIndicator(progress = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun FakePreview() {
    TycheTheme {
        FakePoolItem()
    }
}