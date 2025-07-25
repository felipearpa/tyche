package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScoreFakeModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutScoreDummyModel
import com.felipearpa.tyche.ui.ProgressIndicator
import com.felipearpa.tyche.ui.SwipeToRevealBox
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    var isOptionsRevealed by remember { mutableStateOf(false) }

    SwipeToRevealBox(
        isRevealed = isOptionsRevealed,
        onExpanded = { isOptionsRevealed = true },
        onCollapsed = { isOptionsRevealed = false },
        actions = {
            IconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colorScheme.primaryContainer)
                    .padding(all = LocalBoxSpacing.current.medium),
                onClick = onJoin,
            ) {
                Icon(
                    painter = painterResource(R.drawable.person_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_HEIGHT)
                .background(color = MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = LocalBoxSpacing.current.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small)) {
                    Text(text = poolGamblerScore.poolName, modifier = shimmerModifier)
                    poolGamblerScore.currentPosition?.let {
                        Text(
                            text = stringResource(R.string.position_label, it),
                            modifier = shimmerModifier,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                poolGamblerScore.difference()?.let {
                    Box {
                        ProgressIndicator(
                            shimmerModifier = shimmerModifier,
                            difference = it,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PoolScoreFakeItem(modifier: Modifier = Modifier) {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScoreFakeModel(),
        onJoin = {},
        modifier = modifier,
        shimmerModifier = Modifier.shimmer(),
    )
}

private val MIN_HEIGHT = 48.dp

@Preview(showBackground = true)
@Composable
private fun PoolScoreItemPreview() {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScoreDummyModel(),
        onJoin = {},
        modifier = Modifier.fillMaxWidth(),
    )
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
    PoolScoreFakeItem(modifier = Modifier.fillMaxWidth())
}
