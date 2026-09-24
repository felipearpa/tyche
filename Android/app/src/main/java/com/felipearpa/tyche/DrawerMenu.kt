package com.felipearpa.tyche

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.felipearpa.tyche.ui.PushDrawer
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

/**
 * The hierarchy both drawers share: account identity with Profile directly below it, any
 * [sections], then a separated sign-out footer. The menu is at least as tall as the drawer, so the
 * footer rests at the bottom when there is room, and it scrolls as a whole when large text or a
 * short window leaves none, so every action stays reachable.
 *
 * The drawer container already keeps its content clear of the system bars, so the menu adds no
 * insets of its own.
 */
@Composable
fun DrawerMenu(
    accountId: String,
    username: String,
    email: String,
    onProfile: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    sections: @Composable ColumnScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = if (constraints.hasBoundedHeight) maxHeight else Dp.Unspecified
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight),
        ) {
            AccountHeaderDrawer(
                accountId = accountId,
                username = username,
                email = email,
                onProfile = onProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = LocalBoxSpacing.current.large),
            )

            sections()

            Spacer(modifier = Modifier.height(DrawerSectionGap))
            Spacer(modifier = Modifier.weight(1f))

            SignOutFooter(onSignOut = onSignOut)
        }
    }
}

@Composable
private fun SignOutFooter(onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DrawerButtonRow(
            iconResId = R.drawable.sign_out,
            title = stringResource(id = R.string.sign_out_action),
            onClick = onSignOut,
            modifier = Modifier.padding(vertical = LocalBoxSpacing.current.medium),
        )
    }
}

/**
 * The configurations a drawer view is previewed in, inside an open [DrawerPreviewHost]: light and
 * dark, a wide window, a short window, and the largest font scale. Right-to-left needs its own
 * preview, which [DrawerPreviewHost] takes as a parameter.
 */
@PreviewLightDark
@Preview(name = "Wide window", device = Devices.PIXEL_TABLET)
@Preview(name = "Short window", device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(name = "Largest font", fontScale = 2f)
internal annotation class DrawerPreviews

/** Shows [drawerContent] in an open drawer beside an empty screen. */
@Composable
internal fun DrawerPreviewHost(
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    drawerContent: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        TycheTheme {
            Surface {
                PushDrawer(isOpen = true, onOpenChange = {}, drawerContent = drawerContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
    }
}
