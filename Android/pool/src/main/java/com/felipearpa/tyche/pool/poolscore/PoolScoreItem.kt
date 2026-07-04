package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.PositionIndicator
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScorePlaceholderModel
import com.felipearpa.tyche.pool.poolGamblerScoreWithoutPositionDummyModel
import com.felipearpa.tyche.pool.rank
import com.felipearpa.tyche.ui.TrendIndicator
import com.felipearpa.tyche.ui.shimmer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            ) {
                poolGamblerScore.position?.let {
                    PositionIndicator(
                        position = it,
                        shouldUsePrimaryColor = false,
                        shimmerModifier = placeholderModifier,
                    )
                }
                poolGamblerScore.rank()?.let {
                    TrendIndicator(
                        placeholderModifier = placeholderModifier,
                        rank = it,
                        textStyle = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poolGamblerScore.poolName,
                    modifier = placeholderModifier,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    poolGamblerScore.score?.let {
                        Text(
                            text = stringResource(id = R.string.points_text, it),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = placeholderModifier,
                        )
                    }

                    poolGamblerScore.gamblerCount?.let {
                        Text(
                            text = stringResource(id = R.string.gamblers_text, it),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = placeholderModifier,
                        )
                    }
                }
            }

            IconButton(
                onClick = onJoin,
                modifier = placeholderModifier,
            ) {
                Icon(
                    painter = painterResource(SharedR.drawable.person_add),
                    contentDescription = null,
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

@PreviewLightDark
@Composable
private fun PoolScoreItemPreviewWithoutScore() {
    TycheTheme {
        Surface {
            PoolScoreItem(
                poolGamblerScore = poolGamblerScoreWithoutPositionDummyModel(),
                onJoin = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolScoreItemLongPoolNamePreview() {
    TycheTheme {
        Surface {
            PoolScoreItem(
                poolGamblerScore = poolGamblerScoreDummyModel().copy(
                    poolName = "This is a very long pool name to test how it is displayed in the list",
                ),
                onJoin = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolScoreFakeItemPreview() {
    TycheTheme {
        Surface {
            PoolScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
        }
    }
}
