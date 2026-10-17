package com.felipearpa.tyche.pool.managegamblers

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.account.AccountAvatarFallback
import com.felipearpa.tyche.pool.R
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.loading.BallSpinner
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import com.revenuecat.placeholder.placeholder
import com.felipearpa.tyche.ui.R as SharedR

/**
 * A pool member's avatar, username, and email, in one of the row's states:
 *
 * - The owner (the signed-in gambler on this screen) is drawn as the current user, with "You"
 *   beside the username and an "Owner" pill at the trailing edge.
 * - While a removal runs ([MutationState.Mutating]) the item dims and the shared ball spinner
 *   replaces the avatar.
 * - A failed removal replaces the email with "Couldn't remove", announced politely, and shows
 *   Retry ([onRetry]) and dismiss ([onDismissFailure]) controls at the trailing edge.
 *
 * Loading rows render this same component from `poolMemberPlaceholderModel()` with
 * [isPlaceholder] set; the item then masks the avatar circle and both text leaves with the shared
 * [LocalLoadingPlaceholderPulse], draws no identity, and exposes nothing to TalkBack. Callers
 * pass no effect.
 */
@Composable
fun ManageGamblerItem(
    state: MutationState<PoolMemberModel>,
    onRetry: () -> Unit,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
) {
    val member = state.activeValue()
    val isCurrentUser = member.isOwner && !isPlaceholder
    val isRemoving = state is MutationState.Mutating || state is MutationState.Mutated
    val isFailed = state is MutationState.Failure && !isPlaceholder

    val rowAlpha by animateFloatAsState(
        targetValue = if (isRemoving) REMOVING_ALPHA else 1f,
        label = "rowAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            // Placeholder values are filler, not members: keep them from screen readers.
            .then(if (isPlaceholder) Modifier.clearAndSetSemantics {} else Modifier),
        horizontalArrangement = Arrangement.spacedBy(identitySpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(
            member = member,
            isCurrentUser = isCurrentUser,
            isRemoving = isRemoving,
            isPlaceholder = isPlaceholder,
        )

        MemberText(
            member = member,
            isCurrentUser = isCurrentUser,
            isFailed = isFailed,
            isPlaceholder = isPlaceholder,
            modifier = Modifier.weight(1f),
        )

        if (isCurrentUser) OwnerPill()
        if (isFailed) {
            RemovalFailureActions(
                username = member.gamblerUsername,
                onRetry = onRetry,
                onDismissFailure = onDismissFailure,
            )
        }
    }
}

@Composable
private fun MemberAvatar(
    member: PoolMemberModel,
    isCurrentUser: Boolean,
    isRemoving: Boolean,
    isPlaceholder: Boolean,
) {
    if (isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        AccountAvatar(
            accountId = member.gamblerId,
            fallback = AccountAvatarFallback(identity = member.gamblerUsername),
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .placeholder(
                    color = pulse.color,
                    shape = CircleShape,
                    highlight = pulse.highlight,
                ),
            isPlaceholder = true,
        )
        return
    }

    val extendedColors = LocalExtendedColorScheme.current
    Crossfade(targetState = isRemoving, label = "avatar") { removing ->
        if (removing) {
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
            AccountAvatar(
                accountId = member.gamblerId,
                fallback = AccountAvatarFallback(
                    identity = member.gamblerEmail.substringBefore('@'),
                    colorKey = member.gamblerEmail,
                    backgroundColor = if (isCurrentUser) extendedColors.currentUser else null,
                    foregroundColor = if (isCurrentUser) extendedColors.onCurrentUser else null,
                ),
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
            )
        }
    }
}

/** The username ("You" beside it for the owner), then the email or the removal failure. */
@Composable
private fun MemberText(
    member: PoolMemberModel,
    isCurrentUser: Boolean,
    isFailed: Boolean,
    isPlaceholder: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse = LocalLoadingPlaceholderPulse.current
    // Applied to each text leaf separately, never to the whole row.
    val textMask = if (isPlaceholder) {
        Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(youLabelSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = member.gamblerUsername,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = primaryTextColor(isCurrentUser),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).then(textMask),
            )
            if (isCurrentUser) {
                Text(
                    text = stringResource(id = R.string.manage_gamblers_you_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LocalExtendedColorScheme.current.currentUser,
                    maxLines = 1,
                )
            }
        }
        if (isFailed) {
            RemovalFailureLine(username = member.gamblerUsername)
        } else {
            Text(
                text = member.gamblerEmail,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor(isCurrentUser),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = textMask,
            )
        }
    }
}

/** "Couldn't remove" in place of the email; TalkBack announces it with the gambler's name. */
@Composable
private fun RemovalFailureLine(username: String) {
    val announcement = stringResource(id = R.string.remove_gambler_failure_announcement, "@$username")
    Row(
        horizontalArrangement = Arrangement.spacedBy(failureGlyphSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = announcement
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        Icon(
            painter = painterResource(id = SharedR.drawable.error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(failureGlyphSize),
        )
        Text(
            text = stringResource(id = R.string.remove_gambler_failure_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun RemovalFailureActions(
    username: String,
    onRetry: () -> Unit,
    onDismissFailure: () -> Unit,
) {
    val retryDescription = stringResource(id = R.string.retry_remove_gambler_action, "@$username")
    val dismissDescription = stringResource(id = R.string.dismiss_remove_gambler_failure_action, "@$username")
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(
            onClick = onRetry,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier
                .heightIn(min = minimumControlSize)
                .semantics { contentDescription = retryDescription },
        ) {
            Text(
                text = stringResource(id = SharedR.string.retry_action),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        IconButton(onClick = onDismissFailure) {
            Icon(
                painter = painterResource(id = SharedR.drawable.close),
                contentDescription = dismissDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OwnerPill() {
    val extendedColors = LocalExtendedColorScheme.current
    Surface(
        shape = CircleShape,
        color = extendedColors.currentUser
            .copy(alpha = OWNER_PILL_TINT_ALPHA)
            .compositeOver(extendedColors.currentUserContainer),
        contentColor = extendedColors.currentUser,
    ) {
        Text(
            text = stringResource(id = R.string.manage_gamblers_owner_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun primaryTextColor(isCurrentUser: Boolean): Color =
    if (isCurrentUser) LocalExtendedColorScheme.current.onCurrentUserContainer else MaterialTheme.colorScheme.onSurface

@Composable
private fun secondaryTextColor(isCurrentUser: Boolean): Color =
    if (isCurrentUser) {
        LocalExtendedColorScheme.current.onCurrentUserContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

private val avatarSize = 40.dp
private val identitySpacing = 12.dp
private val youLabelSpacing = 8.dp
private val failureGlyphSize = 16.dp
private val failureGlyphSpacing = 4.dp
private val minimumControlSize = 44.dp
private const val REMOVING_ALPHA = 0.55f

/** Light `currentUser` text keeps 4.5:1 on the pill up to this tint (4.55:1 at 14 %). */
private const val OWNER_PILL_TINT_ALPHA = 0.14f

private fun previewMember(isOwner: Boolean = false) =
    PoolMemberModel(
        gamblerId = "1",
        gamblerUsername = if (isOwner) "felipearpa" else "santiago",
        gamblerEmail = if (isOwner) "felipe@example.com" else "santiago@example.com",
        isOwner = isOwner,
    )

@PreviewLightDark
@Composable
private fun ManageGamblerItemPreview() {
    TycheTheme {
        Surface {
            ManageGamblerItem(
                state = MutationState.Idle(previewMember()),
                onRetry = {},
                onDismissFailure = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerItemOwnerPreview() {
    TycheTheme {
        Surface(color = LocalExtendedColorScheme.current.currentUserContainer) {
            ManageGamblerItem(
                state = MutationState.Idle(previewMember(isOwner = true)),
                onRetry = {},
                onDismissFailure = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerItemRemovingPreview() {
    val member = previewMember()
    TycheTheme {
        Surface {
            ManageGamblerItem(
                state = MutationState.Mutating(original = member, updated = member),
                onRetry = {},
                onDismissFailure = {},
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Failed, large text", fontScale = 2f, widthDp = 360)
@Preview(name = "Failed, Spanish", locale = "es-rCO")
@Composable
private fun ManageGamblerItemFailedPreview() {
    val member = previewMember()
    TycheTheme {
        Surface {
            ManageGamblerItem(
                state = MutationState.Failure(
                    original = member,
                    updated = member,
                    exception = UnknownLocalizedException(),
                ),
                onRetry = {},
                onDismissFailure = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManageGamblerItemPlaceholderPreview() {
    TycheTheme {
        Surface {
            ManageGamblerItem(
                state = MutationState.Idle(poolMemberPlaceholderModel()),
                onRetry = {},
                onDismissFailure = {},
                isPlaceholder = true,
            )
        }
    }
}
