package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.PositionIndicator
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutScoreDummyModel
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderModifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium),
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            poolGamblerScore.position?.let { position ->
                PositionIndicator(
                    position = position,
                    isCurrentUser = true,
                    shimmerModifier = placeholderModifier,
                )
            }

            Text(
                text = poolGamblerScore.poolName,
                modifier = Modifier
                    .weight(1f)
                    .then(placeholderModifier),
            )

            poolGamblerScore.difference()?.let {
                TrendIndicator(
                    shimmerModifier = placeholderModifier,
                    difference = it,
                )
            }

            Spacer(modifier = Modifier.height(LocalBoxSpacing.current.medium))

            IconButton(onClick = onJoin) {
                Icon(
                    painter = painterResource(R.drawable.person_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun PoolScorePlaceholderItem(modifier: Modifier = Modifier) {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScorePlaceholderModel(),
        onJoin = {},
        modifier = modifier,
        placeholderModifier = Modifier.shimmer(),
    )
}

@PreviewLightDark
@Composable
private fun PoolScoreItemPreview() {
    TycheTheme {
        Surface {
            PoolScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel(),
                onJoin = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreItemPreviewWithoutScore() {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScoreWithoutScoreDummyModel(),
        onJoin = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreFakeItemPreview() {
    PoolScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
}
