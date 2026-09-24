package com.felipearpa.tyche

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

/** The drawer's account identity with Profile directly below it. */
@Composable
fun AccountHeaderDrawer(
    accountId: String,
    username: String,
    email: String,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        modifier = modifier,
    ) {
        AccountIdentityRow(
            accountId = accountId,
            username = username,
            email = email,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DrawerGutter),
        )

        DrawerButtonRow(
            iconResId = SharedR.drawable.filled_person,
            title = stringResource(id = R.string.profile_title),
            onClick = onProfile,
        )
    }
}

/**
 * The avatar beside the name and email, read by assistive technology as one element. The avatar
 * spans the drawer's leading column, so the name lines up with the action labels below.
 */
@Composable
fun AccountIdentityRow(
    accountId: String,
    username: String,
    email: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DrawerColumnGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        AccountAvatar(
            accountId = accountId,
            email = email,
            modifier = Modifier
                .size(DrawerLeadingColumnWidth)
                .clip(CircleShape)
                // The name follows; the letter avatar's initial would only repeat it.
                .clearAndSetSemantics {},
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = username.ifEmpty { email },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = drawerSupportingTextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Supporting drawer text, such as the email, section titles, and pool statistics. It stays subdued
 * on the drawer and pool-summary surfaces while keeping at least 4.5:1 contrast on both (5.7:1 or
 * more with Fortuna's light and dark colors).
 */
@Composable
@ReadOnlyComposable
internal fun drawerSupportingTextColor(): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = SupportingTextAlpha)

private const val SupportingTextAlpha = 0.7f

@PreviewLightDark
@Composable
private fun AccountHeaderDrawerPreview() {
    TycheTheme {
        Surface {
            AccountHeaderDrawer(
                accountId = "account-1",
                username = "felipearpa",
                email = "felipearpa@email.com",
                onProfile = {},
            )
        }
    }
}
