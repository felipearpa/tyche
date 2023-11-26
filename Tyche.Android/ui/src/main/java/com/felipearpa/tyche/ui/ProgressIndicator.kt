package com.felipearpa.tyche.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.core.emptyString
import com.felipearpa.tyche.ui.theme.negativeColor
import com.felipearpa.tyche.ui.theme.neutralColor
import com.felipearpa.tyche.ui.theme.positiveColor
import kotlin.math.abs

@Composable
private fun StableProgressIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Icon(
            modifier = shimmerModifier
                .size(24.dp, 24.dp)
                .testTag("stableProgressIndicator"),
            painter = painterResource(id = R.drawable.ic_horizontal_rule),
            contentDescription = emptyString(),
            tint = neutralColor
        )
    }
}

@Composable
private fun UpProgressIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    progress: Int
) {
    Row(modifier = modifier.testTag("upProgressIndicator")) {
        Icon(
            modifier = shimmerModifier.size(24.dp, 24.dp),
            painter = painterResource(id = R.drawable.ic_arrow_upward),
            contentDescription = emptyString(),
            tint = positiveColor
        )
        Text(
            text = abs(progress).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = positiveColor,
            modifier = shimmerModifier
        )
    }
}

@Composable
private fun DownProgressIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    progress: Int
) {
    Row(modifier = modifier.testTag("downProgressIndicator")) {
        Icon(
            modifier = shimmerModifier.size(24.dp, 24.dp),
            painter = painterResource(id = R.drawable.ic_arrow_downward),
            contentDescription = emptyString(),
            tint = negativeColor
        )
        Text(
            text = abs(progress).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = negativeColor,
            modifier = shimmerModifier
        )
    }
}

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    difference: Int?
) {
    difference?.let { differenceValue ->
        when {
            differenceValue > 0 -> UpProgressIndicator(
                modifier = modifier,
                shimmerModifier = shimmerModifier,
                progress = differenceValue
            )

            differenceValue < 0 -> DownProgressIndicator(
                modifier = modifier,
                shimmerModifier = shimmerModifier,
                progress = differenceValue
            )

            else -> StableProgressIndicator(modifier = modifier, shimmerModifier = shimmerModifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StableProgressIndicatorPreview() {
    MaterialTheme {
        StableProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun UpProgressIndicatorPreview() {
    MaterialTheme {
        UpProgressIndicator(progress = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun DownProgressIndicatorPreview() {
    MaterialTheme {
        DownProgressIndicator(progress = 1)
    }
}