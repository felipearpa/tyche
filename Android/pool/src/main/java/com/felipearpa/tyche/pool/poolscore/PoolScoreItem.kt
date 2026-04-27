package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutScoreDummyModel
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium),
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
                    poolGamblerScore.position?.let {
                        Text(
                            text = stringResource(R.string.position_label, it),
                            modifier = shimmerModifier,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                poolGamblerScore.difference()?.let {
                    Box {
                        TrendIndicator(
                            shimmerModifier = shimmerModifier,
                            difference = it,
                        )
                    }
                }

            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onJoin,
                    modifier = shimmerModifier,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person_add),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(text = stringResource(id = R.string.invite_friends_action))
                    }
                }
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
        shimmerModifier = Modifier.shimmer(),
    )
}

@PreviewLightDark
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
    PoolScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
}
