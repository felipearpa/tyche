package com.felipearpa.tyche.pool.managegamblers

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.EmailAvatar
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue

@Composable
fun ManageGamblerItem(
    state: MutationState<PoolMemberModel>,
    modifier: Modifier = Modifier,
) {
    val member = state.activeValue()
    val isDeleting = state is MutationState.Mutating || state is MutationState.Mutated

    val rowAlpha by animateFloatAsState(
        targetValue = if (isDeleting) 0.55f else 1f,
        label = "rowAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha),
        horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(targetState = isDeleting, label = "avatar") { deleting ->
            if (deleting) {
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
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.gamblerUsername,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.gamblerEmail,
                style = MaterialTheme.typography.bodySmall,
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
                state = MutationState.Idle(
                    PoolMemberModel(
                        gamblerId = "1",
                        gamblerUsername = "danielsanto",
                        gamblerEmail = "daniel@example.com",
                    ),
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerItemDeletingPreview() {
    val member = PoolMemberModel(
        gamblerId = "1",
        gamblerUsername = "danielsanto",
        gamblerEmail = "daniel@example.com",
    )
    TycheTheme {
        Surface {
            ManageGamblerItem(
                state = MutationState.Mutating(original = member, updated = member),
            )
        }
    }
}
