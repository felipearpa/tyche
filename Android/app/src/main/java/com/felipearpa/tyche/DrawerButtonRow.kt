package com.felipearpa.tyche

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

/**
 * A drawer action: an icon centered in the drawer's leading column, so labels align with the
 * account name above, and an optional trailing [accessory]. The row spans the drawer's width, grows
 * with the font scale, keeps at least a 48 dp touch target, and shows Material's ripple when
 * pressed. A disabled row keeps its place, dimmed, and ignores taps.
 */
@Composable
fun DrawerButtonRow(
    iconResId: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    accessory: @Composable RowScope.() -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DrawerColumnGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinimumRowHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = DrawerGutter, vertical = RowVerticalPadding)
            .alpha(if (enabled) 1f else DisabledContentAlpha),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(DrawerLeadingColumnWidth),
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(MenuIconSize),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.weight(1f),
        )

        accessory()
    }
}

/** The drawer's outer gutter; drawer text and rows keep this far from its edges. */
internal val DrawerGutter = 16.dp

/**
 * The width of the drawer's leading column: the account avatar, and the menu icons centered
 * below it.
 */
internal val DrawerLeadingColumnWidth = 64.dp

/** Between the leading column and the text beside it. */
internal val DrawerColumnGap = 8.dp

/** Drawer sections, such as the pool summary and its actions, start this far below the last. */
internal val DrawerSectionGap = 24.dp

private val MenuIconSize = 24.dp
private val MinimumRowHeight = 48.dp
private val RowVerticalPadding = 12.dp

/** Material's opacity for disabled content. */
private const val DisabledContentAlpha = 0.38f

@PreviewLightDark
@Composable
private fun DrawerButtonRowPreview() {
    TycheTheme {
        Surface {
            Column {
                DrawerButtonRow(
                    iconResId = SharedR.drawable.filled_person,
                    title = "Profile",
                    onClick = {},
                )
                DrawerButtonRow(
                    iconResId = SharedR.drawable.group,
                    title = "Gamblers",
                    onClick = {},
                    accessory = { Text(text = "19", style = MaterialTheme.typography.labelMedium) },
                )
                DrawerButtonRow(
                    iconResId = SharedR.drawable.delete_forever,
                    title = "Delete pool",
                    onClick = {},
                    tint = MaterialTheme.colorScheme.error,
                    enabled = false,
                )
            }
        }
    }
}
