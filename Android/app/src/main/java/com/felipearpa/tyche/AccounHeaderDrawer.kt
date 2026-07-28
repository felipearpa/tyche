package com.felipearpa.tyche

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.tyche.ui.R as SharedR

@Composable
fun AccountHeaderDrawer(
    accountId: String,
    username: String,
    email: String,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium)) {
        AccountIdentityRow(
            accountId = accountId,
            username = username,
            email = email,
            modifier = modifier,
        )

        DrawerButtonRow(
            iconResId = SharedR.drawable.filled_person,
            title = stringResource(id = R.string.profile_title),
            onClick = onProfile,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AccountIdentityRow(
    accountId: String,
    username: String,
    email: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        AccountAvatar(
            accountId = accountId,
            email = email,
            modifier = Modifier
                .size(AVATAR_SIZE.dp)
                .clip(CircleShape),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.small),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = username.ifEmpty { email },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val AVATAR_SIZE = 64

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
