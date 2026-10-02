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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.foundation.time.toShortDateString
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import com.felipearpa.tyche.ui.R as SharedR

/**
 * A pool template card. Loading cards render this same component from `poolLayoutFakeModel()`
 * with [isPlaceholder] set; the card then masks the name, start date, and arrow with the shared
 * [LocalLoadingPlaceholderPulse] and exposes nothing to TalkBack, while the card keeps its
 * container color. Callers pass no effect and attach no click action to placeholders.
 */
@Composable
fun PoolFromLayoutCreatorItem(
    poolLayout: PoolLayoutModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isPlaceholder: Boolean = false,
) {
    // Applied to each text and glyph leaf separately; the card keeps its container color.
    val leafMask = if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(
            color = pulse.color,
            shape = pulse.shape,
            highlight = pulse.highlight,
        )
    } else {
        Modifier
    }
    val (backgroundColor, onBackgroundColor) =
        if (isSelected) Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        ) else Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )

    Card(
        // Placeholder values are filler, not templates: keep them from screen readers.
        modifier = if (isPlaceholder) modifier.clearAndSetSemantics {} else modifier,
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
                    modifier = leafMask,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(
                        id = R.string.starting_from_date_text,
                        poolLayout.startDateTime.toShortDateString(),
                    ),
                    modifier = leafMask,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Icon(
                painter = painterResource(SharedR.drawable.arrow_forward),
                contentDescription = null,
                modifier = leafMask,
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
