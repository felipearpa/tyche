package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.pool.difference
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.ui.ProgressIndicator
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing

@Composable
fun PoolScoreItem(
    poolGamblerScore: PoolGamblerScoreModel,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small)) {
            Text(text = poolGamblerScore.poolName, modifier = shimmerModifier)
            poolGamblerScore.currentPosition?.let {
                Text(
                    text = stringResource(R.string.position_label, it),
                    modifier = shimmerModifier,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        poolGamblerScore.difference()?.let {
            Box {
                ProgressIndicator(
                    shimmerModifier = shimmerModifier,
                    difference = it
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolScoreItemPreview() {
    PoolScoreItem(
        poolGamblerScore = poolGamblerScoreDummyModel(),
        modifier = Modifier.fillMaxWidth()
    )
}