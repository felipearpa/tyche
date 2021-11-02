package com.pipel.tyche.ui.poollayout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.pipel.core.toLocalDateTimeString
import com.pipel.tyche.R
import com.pipel.tyche.ui.theme.TycheTheme
import com.pipel.tyche.ui.theme.onPrimaryLight
import com.pipel.ui.DividerDecorator
import java.util.*

@Composable
fun PoolLayoutItem(poolLayout: PoolLayoutModel, modifier: Modifier = Modifier, childModifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = poolLayout.name, modifier = childModifier)

        Text(
            text = stringResource(
                R.string.create_until,
                poolLayout.openingEndDateTime.toLocalDateTimeString()
            ),
            color = MaterialTheme.colors.onPrimaryLight,
            modifier = childModifier
        )
    }
}

@Composable
fun FakePoolLayoutItem() {
    DividerDecorator(bottom = 8.dp) {
        PoolLayoutItem(
            poolLayout = PoolLayoutModel(
                poolLayoutId = UUID.randomUUID(),
                name = "South American qualifiers to Qatar 2022 at October",
                openingStartDateTime = Date(),
                openingEndDateTime = Date()
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            childModifier = Modifier.placeholder(
                visible = true,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PoolLayoutItemPreview() {
    TycheTheme {
        PoolLayoutItem(poolsLayoutsModelsForPreview().iterator().next())
    }
}