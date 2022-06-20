package com.pipel.tyche.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pipel.core.empty
import com.pipel.tyche.R
import com.pipel.tyche.ui.theme.TycheTheme
import com.pipel.tyche.ui.theme.downIndicator
import com.pipel.tyche.ui.theme.stableIndicator
import com.pipel.tyche.ui.theme.upIndicator
import kotlin.math.abs

@Composable
private fun StableProgressIndicator(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier
            .size(24.dp, 24.dp)
            .testTag("stableProgressIndicator"),
        painter = painterResource(id = R.drawable.ic_baseline_remove_24),
        contentDescription = String.empty(),
        tint = MaterialTheme.colors.stableIndicator
    )
}

@Composable
private fun UpProgressIndicator(modifier: Modifier = Modifier, progress: Int) {
    Row(modifier = modifier.testTag("upProgressIndicator")) {
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
    Row(modifier = modifier.testTag("downProgressIndicator")) {
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
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    difference: Int?
) {
    difference?.let { differenceValue ->
        when {
            differenceValue > 0 -> UpProgressIndicator(
                modifier = modifier,
                progress = differenceValue
            )
            differenceValue < 0 -> DownProgressIndicator(
                modifier = modifier,
                progress = differenceValue
            )
            else -> StableProgressIndicator(modifier = modifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StableProgressIndicatorPreview() {
    StableProgressIndicator()
}

@Preview(showBackground = true)
@Composable
private fun UpProgressIndicatorPreview() {
    UpProgressIndicator(progress = 1)
}

@Preview(showBackground = true)
@Composable
private fun DownProgressIndicatorPreview() {
    DownProgressIndicator(progress = 1)
}