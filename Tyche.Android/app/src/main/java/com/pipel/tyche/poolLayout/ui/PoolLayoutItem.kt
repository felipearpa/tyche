package com.pipel.tyche.poolLayout.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.pipel.core.toLocalDateTimeString
import com.pipel.core.type.Ulid
import com.pipel.tyche.R
import com.pipel.tyche.poolLayout.view.PoolLayoutModel
import com.pipel.tyche.ui.theme.TycheTheme
import com.pipel.tyche.ui.theme.onPrimaryLight
import java.util.*

@Composable
private fun PoolLayoutItem(
    poolLayout: PoolLayoutModel,
    modifier: Modifier = Modifier,
    childModifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = poolLayout.name, modifier = childModifier)

        Text(
            modifier = childModifier,
            text = stringResource(
                R.string.create_until,
                poolLayout.openingEndDateTime.toLocalDateTimeString()
            ),
            color = MaterialTheme.colors.onPrimaryLight
        )
    }
}

@Composable
fun PoolLayoutItem(poolLayout: PoolLayoutModel, modifier: Modifier) {
    PoolLayoutItem(poolLayout = poolLayout, modifier = modifier, childModifier = Modifier)
}

@Composable
fun FakePoolLayoutItem(modifier: Modifier = Modifier) {
    PoolLayoutItem(
        poolLayout = PoolLayoutModel(
            poolLayoutId = Ulid.randomUlid().toString(),
            name = "South American qualifiers to Qatar 2022 on October",
            openingStartDateTime = Date(),
            openingEndDateTime = Date()
        ),
        modifier = modifier,
        childModifier = Modifier.placeholder(
            visible = true,
            highlight = PlaceholderHighlight.shimmer()
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolLayoutItemPreview() {
    TycheTheme {
        PoolLayoutItem(poolsLayoutsModelsForPreview().iterator().next())
    }
}

@Preview(showBackground = true)
@Composable
private fun FakePoolLayoutItemPreview() {
    TycheTheme {
        FakePoolLayoutItem()
    }
}