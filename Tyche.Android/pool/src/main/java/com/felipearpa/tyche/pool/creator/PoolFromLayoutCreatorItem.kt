package com.felipearpa.tyche.pool.creator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.pool.R

@Composable
fun PoolFromLayoutCreatorItem(
    poolLayout: PoolLayoutModel,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = poolLayout.name,
            modifier = shimmerModifier,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(
                id = R.string.starting_from_date_text,
                poolLayout.startDateTime.toShortDateString(),
            ),
            modifier = shimmerModifier,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolFromLayoutCreatorItemPreview() {
    PoolFromLayoutCreatorItem(
        poolLayout = poolLayoutDummyModel(),
        modifier = Modifier.fillMaxWidth(),
    )
}
