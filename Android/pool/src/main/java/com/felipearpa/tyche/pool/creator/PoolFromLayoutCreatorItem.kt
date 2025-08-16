package com.felipearpa.tyche.pool.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun PoolFromLayoutCreatorItem(
    poolLayout: PoolLayoutModel,
    modifier: Modifier = Modifier,
    shimmerModifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val (backgroundColor, onBackgroundColor) =
        if (isSelected) Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        ) else Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = onBackgroundColor,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = LocalBoxSpacing.current.medium),
        ) {
            Column {
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

            Icon(
                painter = painterResource(SharedR.drawable.arrow_forward),
                contentDescription = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PoolFromLayoutCreatorItemPreview() {
    TycheTheme {
        PoolFromLayoutCreatorItem(
            poolLayout = poolLayoutDummyModel(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewLightDark
@Composable
private fun SelectedPoolFromLayoutCreatorItemPreview() {
    TycheTheme {
        PoolFromLayoutCreatorItem(
            poolLayout = poolLayoutDummyModel(),
            isSelected = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
