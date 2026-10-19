import SwiftUI
import UIKit
import UI
import Core
import DataBet

/// A pending match's card driven by its row holder. VoiceOver hears a save completing or
/// failing as an announcement, posted on the change of state rather than on every redraw, so a
/// card scrolled back into view does not repeat it.
struct PendingBetItemView: View {
    @ObservedObject var holder: PendingBetRowHolder
    /// Focus entered one of the card's score boxes; the list brings the card above the keyboard.
    let onFocusEnter: () -> Void

    @State private var previousState: PendingBetRowState?

    var body: some View {
        PendingBetItem(
            match: holder.match,
            draft: holder.draft,
            rowState: holder.rowState,
            actions: PendingBetItemActions(
                setHome: holder.setHome,
                setAway: holder.setAway,
                step: { team, delta in holder.step(team, by: delta) },
                retry: holder.retry,
                focusEnter: onFocusEnter,
                focusLeave: holder.restoreSavedIfCleared
            )
        )
        .onAppear { previousState = holder.rowState }
        .onChange(of: holder.rowState) { state in
            announceChange(from: previousState, to: state)
            previousState = state
        }
    }

    private func announceChange(from previous: PendingBetRowState?, to state: PendingBetRowState) {
        guard let announcement = saveAnnouncement(from: previous, to: state, match: holder.match, saved: holder.saved) else {
            return
        }
        UIAccessibility.post(notification: .announcement, argument: announcement)
    }
}

/// What VoiceOver hears when a card's state changes: the saved scores when a save completes, the
/// failure when it fails, the lock when the server refuses a save because betting has closed, and
/// nothing otherwise. A lock that arrives with a feed refresh is not announced.
func saveAnnouncement(
    from previous: PendingBetRowState?,
    to state: PendingBetRowState,
    match: PoolGamblerBetModel,
    saved: TeamScore<Int>?
) -> String? {
    switch state {
    case .saved where previous == .saving:
        guard let saved else { return nil }
        return String(localized: .betSavedAnnouncement(
            match.homeTeamName, saved.homeTeamValue, match.awayTeamName, saved.awayTeamValue
        ))
    case .failed:
        return String(localized: .betSaveFailureAnnouncement(match.homeTeamName, match.awayTeamName))
    case .locked where previous == .saving:
        return String(localized: .betLockedAnnouncement(match.homeTeamName, match.awayTeamName))
    default:
        return nil
    }
}

#Preview("No bet") {
    PendingBetItemView(holder: previewRow(poolGamblerBetDummyModel().copy {
        $0.isComputed = false
        $0.betScore = nil
    }), onFocusEnter: {})
    .padding()
}

#Preview("Saved") {
    PendingBetItemView(holder: previewRow(poolGamblerBetDummyModel().copy { $0.isComputed = false }), onFocusEnter: {})
        .padding()
}

#Preview("Locked") {
    PendingBetItemView(holder: previewRow(poolGamblerBetDummyModel().copy {
        $0.isComputed = false
        $0.lock()
    }), onFocusEnter: {})
    .padding()
}

/// A row as the Bets list creates it, saving to the fake repository.
@MainActor
private func previewRow(_ match: PoolGamblerBetModel) -> PendingBetRowHolder {
    let repository = PoolGamblerBetFakeRepository()
    let list = PendingBetListViewModel(
        getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(poolGamblerBetRepository: repository),
        betUseCase: BetUseCase(poolGamblerBetRepository: repository),
        gamblerId: match.gamblerId,
        poolId: match.poolId
    )
    return list.rowHolder(for: match)
}
