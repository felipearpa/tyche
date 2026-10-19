package com.felipearpa.tyche.bet.pending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.R
import com.felipearpa.tyche.bet.finished.rememberHistoryMatchDateFormat
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Draft
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Team
import com.felipearpa.tyche.bet.poolGamblerBetFakeModel
import com.felipearpa.tyche.ui.FlagImage
import com.felipearpa.tyche.ui.theme.LocalLoadingPlaceholderPulse
import com.felipearpa.tyche.ui.theme.TycheTheme
import com.revenuecat.placeholder.placeholder
import kotlinx.datetime.LocalDateTime

/** What an open card's controls do. Locked and placeholder cards have none. */
internal data class PendingBetItemActions(
    val setHome: (Int?) -> Unit,
    val setAway: (Int?) -> Unit,
    val step: (Team, Int) -> Unit,
    val retry: () -> Unit,
    /** Focus left both score boxes. */
    val focusLeave: () -> Unit,
)

/**
 * One pending match as a card: kickoff time, the "Locks in N min" note ([lockCountdown]) with an
 * amber outline during the last hour before the lock, the status pill, both teams' flags and
 * names, and the score area.
 *
 * - Open cards ([actions] given, any state but [RowState.LOCKED]) show `+`/`−` steppers around
 *   two typeable score boxes; typing the first digit into the home box moves focus to the away
 *   box. An open card keeps one height in every state: only [RowState.FAILED] adds a row, the
 *   banner with Retry. On an incomplete card the missing box tells TalkBack whose score is
 *   needed to save. Leaving both boxes (focus moving between them does not count) calls
 *   [PendingBetItemActions.focusLeave].
 * - Locked cards show the scores read-only in muted boxes, a dash where there is no prediction,
 *   with no controls.
 * - The status pill, boxes, and steppers keep their sizes in every state. The card's height
 *   changes only when the match locks (the steppers leave) or the failure banner comes or goes;
 *   those rows expand and shrink with a fade, a fade only with reduced motion.
 *
 * The kickoff line is the card's summary for TalkBack: the matchup, time, and lock note as its
 * description and the status, Saving included, as its state description. [announcement] is
 * spoken politely when the card reports a save, a failure, or the lock.
 *
 * Loading slots render this same component from `poolGamblerBetFakeModel()` with
 * [isPlaceholder] set: it takes the locked layout, so it has no controls, masks each content leaf
 * with the shared [LocalLoadingPlaceholderPulse], and exposes nothing to TalkBack.
 */
@Composable
internal fun PendingBetItem(
    match: PoolGamblerBetModel,
    draft: Draft,
    rowState: RowState,
    lockCountdown: Int?,
    modifier: Modifier = Modifier,
    actions: PendingBetItemActions? = null,
    announcement: String? = null,
    isPlaceholder: Boolean = false,
) {
    val colors = pendingBetColors()
    val editActions = actions?.takeIf { !isPlaceholder && rowState != RowState.LOCKED }
    val currentActions by rememberUpdatedState(actions)
    var focusedTeam by remember { mutableStateOf<Team?>(null) }
    val hasFocus = focusedTeam != null
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val shape = RoundedCornerShape(PendingBetStyle.cardCornerRadius)
    val outline by animateColorAsState(
        targetValue = if (lockCountdown != null && !isPlaceholder) colors.amber else Color.Transparent,
        animationSpec = PendingBetStyle.spec(),
        label = "lockOutline",
    )

    // Moving from one box to the other loses and gains focus within one event, so only leaving
    // both boxes reaches a composition without focus.
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(hasFocus) {
        if (hadFocus && !hasFocus) currentActions?.focusLeave?.invoke()
        hadFocus = hasFocus
    }
    var cardHeight by remember { mutableIntStateOf(0) }
    KeepVisibleAboveKeyboard(hasFocus = hasFocus, bringIntoViewRequester = bringIntoViewRequester, cardHeight = { cardHeight })
    ClearFocusWhenKeyboardCloses(hasFocus = hasFocus, focusManager = LocalFocusManager.current)
    // A card scrolled away while typing leaves its boxes as well.
    DisposableEffect(Unit) {
        onDispose { if (focusedTeam != null) currentActions?.focusLeave?.invoke() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(PendingBetStyle.cardSpacing),
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged { size -> cardHeight = size.height }
            .clip(shape)
            .background(colors.cardFill)
            .border(1.dp, outline, shape)
            .then(if (isPlaceholder) Modifier.clearAndSetSemantics {} else Modifier)
            .padding(horizontal = PendingBetStyle.cardHorizontalPadding, vertical = PendingBetStyle.cardVerticalPadding),
    ) {
        PendingBetHeader(
            match = match,
            rowState = rowState,
            lockCountdown = lockCountdown,
            announcement = announcement,
            isPlaceholder = isPlaceholder,
        )
        TeamsAndScores(
            match = match,
            draft = draft,
            editActions = editActions,
            focusedTeam = focusedTeam,
            onBoxFocusChange = { team, focused ->
                if (focused) focusedTeam = team else if (focusedTeam == team) focusedTeam = null
            },
            isPlaceholder = isPlaceholder,
        )
        BelowScores(rowState = rowState, editActions = editActions)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingBetHeader(
    match: PoolGamblerBetModel,
    rowState: RowState,
    lockCountdown: Int?,
    announcement: String?,
    isPlaceholder: Boolean,
) {
    val colors = pendingBetColors()
    val time = rememberHistoryMatchDateFormat().time(match.matchDateTime)
    val lockNote = lockCountdown?.let { minutes -> pluralStringResource(R.plurals.bet_locks_in_minutes, minutes, minutes) }
    val matchup = stringResource(R.string.match_bets_matchup_accessibility, match.homeTeamName, match.awayTeamName)
    val summary = listOfNotNull(matchup, time, lockNote).joinToString(separator = ", ")
    val status = stringResource(rowState.statusText)
    val mask = Modifier.placeholderMask(isPlaceholder)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    contentDescription = summary
                    stateDescription = status
                },
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                modifier = mask,
            )
            AnimatedVisibility(
                visible = lockNote != null && !isPlaceholder,
                enter = fadeIn(PendingBetStyle.spec()),
                exit = fadeOut(PendingBetStyle.spec()),
            ) {
                Text(
                    text = lockNote.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.amber,
                )
            }
        }
        PendingBetStatusPill(rowState = rowState, isPlaceholder = isPlaceholder, announcement = announcement)
    }
}

@Composable
private fun TeamsAndScores(
    match: PoolGamblerBetModel,
    draft: Draft,
    editActions: PendingBetItemActions?,
    focusedTeam: Team?,
    onBoxFocusChange: (Team, Boolean) -> Unit,
    isPlaceholder: Boolean,
) {
    val scores = @Composable {
        Scores(
            match = match,
            draft = draft,
            editActions = editActions,
            focusedTeam = focusedTeam,
            onBoxFocusChange = onBoxFocusChange,
            isPlaceholder = isPlaceholder,
        )
    }
    // At large text sizes each team takes its own line above the scores, so names wrap between
    // words instead of inside them.
    if (LocalDensity.current.fontScale >= LARGE_TEXT_SCALE) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PendingBetTeam(teamId = match.homeTeamId, teamName = match.homeTeamName, isRow = true, isPlaceholder = isPlaceholder)
            PendingBetTeam(teamId = match.awayTeamId, teamName = match.awayTeamName, isRow = true, isPlaceholder = isPlaceholder)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { scores() }
        }
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        PendingBetTeam(teamId = match.homeTeamId, teamName = match.homeTeamName, isRow = false, isPlaceholder = isPlaceholder, modifier = Modifier.weight(1f))
        scores()
        PendingBetTeam(teamId = match.awayTeamId, teamName = match.awayTeamName, isRow = false, isPlaceholder = isPlaceholder, modifier = Modifier.weight(1f))
    }
}

private const val LARGE_TEXT_SCALE = 1.5f

@Composable
private fun Scores(
    match: PoolGamblerBetModel,
    draft: Draft,
    editActions: PendingBetItemActions?,
    focusedTeam: Team?,
    onBoxFocusChange: (Team, Boolean) -> Unit,
    isPlaceholder: Boolean,
) {
    val awayFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreColumn(
            team = Team.HOME,
            match = match,
            draft = draft,
            editActions = editActions,
            isFocused = focusedTeam == Team.HOME,
            onFocusChange = { focused -> onBoxFocusChange(Team.HOME, focused) },
            onInput = { value ->
                editActions?.setHome?.invoke(value)
                if (value != null && focusedTeam == Team.HOME) awayFocusRequester.requestFocus()
            },
            onImeAction = { awayFocusRequester.requestFocus() },
            focusRequester = remember { FocusRequester() },
            isPlaceholder = isPlaceholder,
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = pendingBetColors().secondaryText,
            modifier = Modifier.placeholderMask(isPlaceholder).clearAndSetSemantics {},
        )
        ScoreColumn(
            team = Team.AWAY,
            match = match,
            draft = draft,
            editActions = editActions,
            isFocused = focusedTeam == Team.AWAY,
            onFocusChange = { focused -> onBoxFocusChange(Team.AWAY, focused) },
            onInput = { value -> editActions?.setAway?.invoke(value) },
            onImeAction = { focusManager.clearFocus() },
            focusRequester = awayFocusRequester,
            isPlaceholder = isPlaceholder,
        )
    }
}

@Composable
private fun ScoreColumn(
    team: Team,
    match: PoolGamblerBetModel,
    draft: Draft,
    editActions: PendingBetItemActions?,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onInput: (Int?) -> Unit,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester,
    isPlaceholder: Boolean,
) {
    val value = if (team == Team.HOME) draft.home else draft.away
    val otherValue = if (team == Team.HOME) draft.away else draft.home
    val teamName = if (team == Team.HOME) match.homeTeamName else match.awayTeamName
    val reduceMotion = reduceMotion()
    val isEditable = editActions != null

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = isEditable,
            enter = PendingBetStyle.enterBelow(reduceMotion),
            exit = PendingBetStyle.exitBelow(reduceMotion),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BetScoreStepper(isIncrease = true, teamName = teamName, onClick = { editActions?.step?.invoke(team, 1) })
                Spacer(modifier = Modifier.height(STEPPER_GAP))
            }
        }
        Crossfade(targetState = isEditable, animationSpec = PendingBetStyle.spec(), label = "scoreBox") { editable ->
            if (editable) {
                BetScoreBox(
                    value = value,
                    otherValue = otherValue,
                    teamName = teamName,
                    isFocused = isFocused,
                    imeAction = if (team == Team.HOME) ImeAction.Next else ImeAction.Done,
                    onInput = onInput,
                    onFocusChange = onFocusChange,
                    onImeAction = onImeAction,
                    focusRequester = focusRequester,
                )
            } else {
                BetScoreReadOnlyBox(value = value, teamName = teamName, modifier = Modifier.placeholderMask(isPlaceholder))
            }
        }
        AnimatedVisibility(
            visible = isEditable,
            enter = PendingBetStyle.enterBelow(reduceMotion),
            exit = PendingBetStyle.exitBelow(reduceMotion),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(STEPPER_GAP))
                BetScoreStepper(isIncrease = false, teamName = teamName, onClick = { editActions?.step?.invoke(team, -1) })
            }
        }
    }
}

/** Steppers reserve 48 dp for touch around their 44-dp shape, so 4 dp more leaves a 6-dp visual gap. */
private val STEPPER_GAP = 4.dp

/** The failure banner, the only row below the scores. */
@Composable
private fun BelowScores(
    rowState: RowState,
    editActions: PendingBetItemActions?,
) {
    val reduceMotion = reduceMotion()
    val enter = PendingBetStyle.enterBelow(reduceMotion)
    val exit = PendingBetStyle.exitBelow(reduceMotion)
    AnimatedVisibility(visible = editActions != null && rowState == RowState.FAILED, enter = enter, exit = exit) {
        BetSaveFailureBanner(onRetry = { editActions?.retry?.invoke() })
    }
}

/**
 * A team's flag over its name beside the scores, or the flag before the name on its own line
 * ([isRow]). The summary and the score controls name the team for TalkBack.
 */
@Composable
private fun PendingBetTeam(teamId: String, teamName: String, isRow: Boolean, isPlaceholder: Boolean, modifier: Modifier = Modifier) {
    val mask = Modifier.placeholderMask(isPlaceholder)
    val flag = @Composable { FlagImage(teamId = teamId, modifier = Modifier.size(PendingBetStyle.flagSize).then(mask)) }
    val name = @Composable {
        Text(
            text = teamName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = pendingBetColors().primaryText,
            textAlign = if (isRow) TextAlign.Start else TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = mask,
        )
    }
    if (isRow) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        ) {
            flag()
            name()
        }
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.clearAndSetSemantics {},
    ) {
        flag()
        name()
    }
}

/** The shared pulse over one content leaf of a placeholder card, never over the whole card. */
@Composable
private fun Modifier.placeholderMask(isPlaceholder: Boolean): Modifier {
    if (!isPlaceholder) return this
    val pulse = LocalLoadingPlaceholderPulse.current
    return placeholder(color = pulse.color, shape = pulse.shape, highlight = pulse.highlight)
}

/** Filler scores for loading cards, as wide as any real score; never shown as real content. */
internal val pendingBetPlaceholderDraft = Draft(home = 88, away = 88)

/** A loading card: the production card from the placeholder model. */
@Composable
internal fun PendingBetPlaceholderItem(modifier: Modifier = Modifier) {
    PendingBetItem(
        match = poolGamblerBetFakeModel(),
        draft = pendingBetPlaceholderDraft,
        rowState = RowState.LOCKED,
        lockCountdown = null,
        isPlaceholder = true,
        modifier = modifier,
    )
}

private val previewMatch = PoolGamblerBetModel(
    poolId = "pool",
    gamblerId = "gambler",
    gamblerUsername = "gambler",
    matchId = "match",
    homeTeamId = "co",
    homeTeamName = "Colombia",
    awayTeamId = "pt",
    awayTeamName = "Portugal",
    matchScore = null,
    betScore = null,
    score = null,
    matchDateTime = LocalDateTime(year = 2026, month = 10, day = 21, hour = 14, minute = 0),
    isLocked = false,
    isComputed = false,
)

private val previewActions = PendingBetItemActions(
    setHome = {},
    setAway = {},
    step = { _, _ -> },
    retry = {},
    focusLeave = {},
)

@Composable
private fun PendingBetItemStates() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        PendingBetItem(match = previewMatch, draft = Draft(null, null), rowState = RowState.EMPTY, lockCountdown = null, actions = previewActions)
        PendingBetItem(match = previewMatch, draft = Draft(2, null), rowState = RowState.INCOMPLETE, lockCountdown = null, actions = previewActions)
        PendingBetItem(match = previewMatch, draft = Draft(2, 1), rowState = RowState.SAVING, lockCountdown = null, actions = previewActions)
        PendingBetItem(match = previewMatch, draft = Draft(2, 1), rowState = RowState.SAVED, lockCountdown = null, actions = previewActions)
        PendingBetItem(match = previewMatch, draft = Draft(2, 1), rowState = RowState.FAILED, lockCountdown = null, actions = previewActions)
        PendingBetItem(match = previewMatch, draft = Draft(null, null), rowState = RowState.EMPTY, lockCountdown = 8, actions = previewActions)
        PendingBetItem(match = previewMatch.copy(isLocked = true), draft = Draft(2, 0), rowState = RowState.LOCKED, lockCountdown = null)
        PendingBetItem(match = previewMatch.copy(isLocked = true), draft = Draft(null, null), rowState = RowState.LOCKED, lockCountdown = null)
        PendingBetPlaceholderItem()
    }
}

@PreviewLightDark
@Composable
private fun PendingBetItemStatesPreview() {
    TycheTheme {
        Surface {
            PendingBetItemStates()
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun PendingBetItemStatesLargeTextPreview() {
    TycheTheme {
        Surface {
            PendingBetItemStates()
        }
    }
}

@Preview(locale = "es")
@Composable
private fun PendingBetItemStatesSpanishPreview() {
    TycheTheme {
        Surface {
            PendingBetItemStates()
        }
    }
}
