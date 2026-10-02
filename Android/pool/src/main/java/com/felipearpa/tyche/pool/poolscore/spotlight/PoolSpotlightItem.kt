package com.felipearpa.tyche.pool.poolscore.spotlight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel

@Composable
fun PoolSpotlightItem(
    modifier: Modifier = Modifier,
    poolGamblerScore: PoolGamblerScoreModel
) {
    Column(modifier = modifier) {
        Text(
            text = poolGamblerScore.poolName,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = poolGamblerScore.gamblerUsername,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolSpotlightItemPreview() {
    Surface {
        PoolSpotlightItem(
            poolGamblerScore = poolGamblerScoreDummyModel(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
