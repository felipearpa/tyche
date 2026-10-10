package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.account.AccountAvatar
import com.felipearpa.tyche.account.AccountAvatarFallback
import com.felipearpa.tyche.bet.MATCH_PREVIEW_VIEWER_ID
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryPointsPill
import com.felipearpa.tyche.bet.finished.HistoryPointsPillSurface
import com.felipearpa.tyche.bet.finished.HistoryRowDivider
import com.felipearpa.tyche.bet.finished.HistoryStyle
import com.felipearpa.tyche.bet.finished.historyPointsShortText
import com.felipearpa.tyche.bet.finished.isPositive
import com.felipearpa.tyche.bet.matchGamblerBetPlaceholderModel
import com.felipearpa.tyche.bet.matchParticipantPreviewModels
import com.felipearpa.tyche.ui.theme.LocalExtendedColorScheme
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder

/**
 * One participant in the match's ranked list: the shared account avatar, their name, their
 * prediction or "No bet", and, once the match is computed, this match's award. The award is the
 * match's points, never the pool total that orders the list.
 *
 * The signed-in gambler's row ([isCurrentUser]) adds the "You" marker and the current-user
 * highlight in its ranked position, and takes no [onClick]. With [onClick] the whole row is one
 * native click target with Material indication. The row is one TalkBack announcement.
 *
 * Loading slots render this same component from [matchGamblerBetPlaceholderModel] with
 * [isPlaceholder] set: each content leaf is then masked with the shared
 * [LocalLoadingPlaceholderPulse], the avatar is not requested, the row ignores taps, and nothing
 * reaches TalkBack.
 */
@Composable
fun MatchGamblerBetItem(
    poolGamblerBet: PoolGamblerBetModel,
    modifier: Modifier = Modifier,
    isCurrentUser: Boolean = false,
    isPlaceholder: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = rowColors(isCurrentUser = isCurrentUser && !isPlaceholder)
    val announcement = participantAnnouncement(poolGamblerBet, isCurrentUser)
    val interaction = if (!isPlaceholder && onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val content = RowContent(
        poolGamblerBet = poolGamblerBet,
        isCurrentUser = isCurrentUser,
        isPlaceholder = isPlaceholder,
        colors = colors,
    )

    val rowModifier = modifier
        .then(interaction)
        .clearAndSetSemantics { if (!isPlaceholder) contentDescription = announcement }
        .background(colors.background)
        .padding(horizontal = HistoryStyle.horizontalPadding, vertical = VERTICAL_PADDING)

    if (LocalDensity.current.fontScale >= STACKED_FONT_SCALE) {
        StackedRow(content = content, modifier = rowModifier)
    } else {
        RegularRow(content = content, modifier = rowModifier)
    }
}

private data class RowColors(val background: Color, val primary: Color, val secondary: Color)

@Composable
private fun rowColors(isCurrentUser: Boolean): RowColors {
    if (!isCurrentUser) {
        return RowColors(Color.Transparent, HistoryStyle.primaryText, HistoryStyle.secondaryText)
    }
    val extended = LocalExtendedColorScheme.current
    return RowColors(extended.currentUserContainer, extended.onCurrentUserContainer, extended.onCurrentUserContainer)
}

private data class RowContent(
    val poolGamblerBet: PoolGamblerBetModel,
    val isCurrentUser: Boolean,
    val isPlaceholder: Boolean,
    val colors: RowColors,
)

/** Avatar, name, prediction, and award on one line; long names wrap. */
@Composable
private fun RegularRow(content: RowContent, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
        modifier = modifier,
    ) {
        Avatar(content)
        Identity(content = content, modifier = Modifier.weight(1f))
        Prediction(content)
        Award(content = content, reservesWidth = true)
    }
}

/** At large font scales the prediction and award move below the name and wrap as needed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StackedRow(content: RowContent, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
        modifier = modifier,
    ) {
        Avatar(content)
        Column(verticalArrangement = Arrangement.spacedBy(STACKED_SPACING), modifier = Modifier.weight(1f)) {
            Identity(content = content)
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(STACKED_SPACING),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Prediction(content = content, modifier = Modifier.padding(end = COLUMN_SPACING))
                Award(content = content, reservesWidth = false)
            }
        }
    }
}

@Composable
private fun Avatar(content: RowContent) {
    val extended = LocalExtendedColorScheme.current
    val size: Dp = minOf(AVATAR_SIZE * LocalDensity.current.fontScale, MAXIMUM_AVATAR_SIZE)
    val bet = content.poolGamblerBet
    val mask = if (content.isPlaceholder) {
        val pulse = LocalLoadingPlaceholderPulse.current
        Modifier.placeholder(color = pulse.color, shape = CircleShape, highlight = pulse.highlight)
    } else {
        Modifier
    }
    AccountAvatar(
        // A placeholder never names an account, so no avatar is requested for filler.
        accountId = if (content.isPlaceholder) "" else bet.gamblerId,
        // The leaderboard's fallback, so the letter avatar keeps the same letter and color.
        fallback = AccountAvatarFallback(
            identity = bet.gamblerUsername,
            colorKey = bet.gamblerUsername,
            backgroundColor = if (content.isCurrentUser) extended.currentUser else null,
            foregroundColor = if (content.isCurrentUser) extended.onCurrentUser else null,
        ),
        isPlaceholder = content.isPlaceholder,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(mask),
    )
}

@Composable
private fun Identity(content: RowContent, modifier: Modifier = Modifier) {
    val mask = leafMask(content.isPlaceholder)
    Column(verticalArrangement = Arrangement.spacedBy(IDENTITY_SPACING), modifier = modifier) {
        Text(
            text = breakableName(content.poolGamblerBet.gamblerUsername),
            style = MaterialTheme.typography.bodyLarge,
            color = content.colors.primary,
            modifier = mask,
        )
        if (content.isCurrentUser) {
            Text(
                text = stringResource(R.string.match_bets_you_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content.colors.primary,
                modifier = mask,
            )
        }
    }
}

@Composable
private fun Prediction(content: RowContent, modifier: Modifier = Modifier) {
    val betScore = content.poolGamblerBet.betScore
    Text(
        text = betScore?.let { "${it.homeTeamValue} – ${it.awayTeamValue}" }
            ?: stringResource(R.string.history_no_bet_label),
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        fontWeight = if (betScore == null) FontWeight.Normal else FontWeight.SemiBold,
        color = if (betScore == null) content.colors.secondary else content.colors.primary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        modifier = modifier.then(leafMask(content.isPlaceholder)),
    )
}

/** This match's award once computed; an uncomputed match shows no award. */
@Composable
private fun Award(content: RowContent, reservesWidth: Boolean) {
    if (!content.poolGamblerBet.isComputed) return
    val points = HistoryPoints.of(content.poolGamblerBet.score)
    HistoryPointsPill(
        text = historyPointsShortText(points),
        isPositive = points.isPositive,
        surface = if (content.isCurrentUser) HistoryPointsPillSurface.CURRENT_USER_ROW else HistoryPointsPillSurface.CANVAS,
        reservedText = if (reservesWidth) {
            pluralStringResource(R.plurals.history_positive_points_short, RESERVED_AWARD_VALUE, RESERVED_AWARD_VALUE)
        } else {
            null
        },
        isPlaceholder = content.isPlaceholder,
    )
}

@Composable
private fun leafMask(isPlaceholder: Boolean): Modifier {
    if (!isPlaceholder) return Modifier
    val pulse = LocalLoadingPlaceholderPulse.current
    return Modifier.placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
}

/**
 * The row's single TalkBack announcement: full name, "You" for the signed-in gambler, the
 * team-qualified prediction or its absence, and the award once computed.
 */
@Composable
internal fun participantAnnouncement(poolGamblerBet: PoolGamblerBetModel, isCurrentUser: Boolean): String {
    val betScore = poolGamblerBet.betScore
    val prediction = if (betScore == null) {
        stringResource(R.string.history_no_bet_label)
    } else {
        stringResource(
            R.string.match_bets_participant_bet_accessibility,
            poolGamblerBet.homeTeamName,
            betScore.homeTeamValue,
            poolGamblerBet.awayTeamName,
            betScore.awayTeamValue,
        )
    }
    val award = if (poolGamblerBet.isComputed) {
        when (val points = HistoryPoints.of(poolGamblerBet.score)) {
            is HistoryPoints.Earned ->
                pluralStringResource(R.plurals.history_points_accessibility, points.value, points.value)

            HistoryPoints.Unavailable -> stringResource(R.string.history_points_unavailable_accessibility)
        }
    } else {
        null
    }
    return listOfNotNull(
        poolGamblerBet.gamblerUsername,
        stringResource(R.string.match_bets_you_label).takeIf { isCurrentUser },
        prediction,
        award,
    ).joinToString(separator = ", ")
}

/**
 * Display names are often email addresses. A line break opportunity after "@" lets a long address
 * wrap there instead of mid-word. Spoken text uses the plain name.
 */
internal fun breakableName(name: String): String = name.replace("@", "@​")

/** The widest award the pill column reserves, so pills line up in most rows. */
private const val RESERVED_AWARD_VALUE = 88
private val VERTICAL_PADDING = 12.dp
private val COLUMN_SPACING = 12.dp
private val STACKED_SPACING = 8.dp
private val IDENTITY_SPACING = 2.dp
private val AVATAR_SIZE = 40.dp
private val MAXIMUM_AVATAR_SIZE = 64.dp

/** At or above this font scale the prediction and award move below the name. */
private const val STACKED_FONT_SCALE = 1.5f

@PreviewLightDark
@Preview(locale = "es-rCO", fontScale = 2f)
@Composable
private fun MatchGamblerBetItemPreview() {
    TycheTheme {
        Surface {
            Column {
                listOf(true, false).forEach { isComputed ->
                    matchParticipantPreviewModels(isComputed).forEach { bet ->
                        val isCurrentUser = bet.gamblerId == MATCH_PREVIEW_VIEWER_ID
                        MatchGamblerBetItem(
                            poolGamblerBet = bet,
                            isCurrentUser = isCurrentUser,
                            onClick = if (isCurrentUser) null else ({}),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        HistoryRowDivider()
                    }
                }
                MatchGamblerBetItem(
                    poolGamblerBet = matchGamblerBetPlaceholderModel(isComputed = true),
                    isPlaceholder = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
