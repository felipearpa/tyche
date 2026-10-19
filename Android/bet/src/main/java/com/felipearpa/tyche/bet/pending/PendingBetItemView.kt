package com.felipearpa.tyche.bet.pending

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.ui.state.activeValue

/**
 * A pending match's card driven by its [rowHolder], which saves the draft automatically.
 * TalkBack hears a save completing or failing, and the lock after a refused save, once, on the
 * change of state: a card scrolled back into view does not repeat it.
 */
@Composable
internal fun PendingBetItemView(
    rowHolder: PendingBetRowHolder,
    lockCountdown: Int?,
    modifier: Modifier = Modifier,
) {
    val match by rowHolder.match.collectAsState()
    val draft by rowHolder.draft.collectAsState()
    val saveState by rowHolder.saveState.collectAsState()
    val rowState = PendingBetRowHolder.rowStateOf(match, draft, saveState)

    var previousState by remember { mutableStateOf<RowState?>(null) }
    var announcement by remember { mutableStateOf<PendingBetAnnouncement?>(null) }
    LaunchedEffect(rowState) {
        announcement = previousState?.let { previous -> PendingBetAnnouncement.of(previous, rowState, saveState.activeValue()) }
        previousState = rowState
    }

    val actions = remember(rowHolder) {
        PendingBetItemActions(
            setHome = rowHolder::setHome,
            setAway = rowHolder::setAway,
            step = rowHolder::step,
            retry = rowHolder::retry,
            focusLeave = rowHolder::restoreSavedIfCleared,
        )
    }
    PendingBetItem(
        match = match,
        draft = draft,
        rowState = rowState,
        lockCountdown = lockCountdown,
        actions = actions,
        announcement = announcement?.text(match),
        modifier = modifier,
    )
}
