package com.felipearpa.tyche.bet.timeline

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.account.AccountAvatarFallback
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

/**
 * The selected gambler's identity at the top of their Timeline: the shared account avatar and
 * their display name. The name comes from navigation, so it is always real content; the avatar
 * loads through the shared avatar store and falls back to the shared letter avatar. The avatar is
 * decorative next to the name, which is the screen's heading.
 */
@Composable
fun TimelineGamblerHeading(
    gamblerId: String,
    gamblerUsername: String,
    modifier: Modifier = Modifier,
) {
    val avatarSize: Dp = minOf(AVATAR_SIZE * LocalDensity.current.fontScale, MAXIMUM_AVATAR_SIZE)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AVATAR_NAME_SPACING),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HistoryStyle.horizontalPadding)
            .padding(top = LocalBoxSpacing.current.large),
    ) {
        AccountAvatar(
            accountId = gamblerId,
            // The leaderboard's fallback, so the letter avatar keeps the same letter and color.
            fallback = AccountAvatarFallback(identity = gamblerUsername, colorKey = gamblerUsername),
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .clearAndSetSemantics {},
        )
        Text(
            text = gamblerUsername,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = HistoryStyle.primaryText,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
    }
}

private val AVATAR_SIZE = 44.dp
private val MAXIMUM_AVATAR_SIZE = 72.dp
private val AVATAR_NAME_SPACING = 12.dp

@PreviewLightDark
@Preview(fontScale = 2f)
@Composable
private fun TimelineGamblerHeadingPreview() {
    TycheTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TimelineGamblerHeading(gamblerId = "", gamblerUsername = "El mono")
                TimelineGamblerHeading(gamblerId = "", gamblerUsername = "giraldocano517@gmail.com")
                TimelineGamblerHeading(
                    gamblerId = "",
                    gamblerUsername = "Juan Pablo Rojas de la Santísima Trinidad",
                )
            }
        }
    }
}
