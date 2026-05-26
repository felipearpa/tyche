package com.felipearpa.tyche.pool.managegamblers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.EmailAvatar
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

@Composable
fun ManageGamblerItem(
    member: PoolMemberModel,
    isDeleting: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDeleting) 0.55f else 1f),
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isDeleting) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(avatarSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BallSpinner(modifier = Modifier.size(avatarSize * 0.6f))
                }
            }
        } else {
            EmailAvatar(
                email = member.gamblerEmail,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@${member.gamblerUsername}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isDeleting) {
                    stringResource(id = R.string.removing_gambler_text)
                } else {
                    member.gamblerEmail
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val avatarSize = 40.dp

@PreviewLightDark
@Composable
private fun ManageGamblerItemPreview() {
    TycheTheme {
        Surface {
            ManageGamblerItem(
                member = PoolMemberModel(
                    gamblerId = "1",
                    gamblerUsername = "danielsanto",
                    gamblerEmail = "daniel@example.com",
                ),
                isDeleting = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerItemDeletingPreview() {
    TycheTheme {
        Surface {
            ManageGamblerItem(
                member = PoolMemberModel(
                    gamblerId = "1",
                    gamblerUsername = "danielsanto",
                    gamblerEmail = "daniel@example.com",
                ),
                isDeleting = true,
            )
        }
    }
}
