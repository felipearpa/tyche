import SwiftUI
import UIKit
import UI
import ViewingState

/// A non-owner member row. A row the owner acted on observes its `GamblerRowHolder`, so it redraws
/// as the removal progresses and its Retry and dismiss controls reach the holder; a row without a
/// holder is idle.
struct GamblerRowStateView: View {
    let member: PoolMemberModel
    let holder: GamblerRowHolder?
    let onRequestRemove: (PoolMemberModel) -> Void

    var body: some View {
        if let holder {
            ObservedGamblerRow(holder: holder, onRequestRemove: onRequestRemove)
        } else {
            RemovableGamblerRow(
                state: .idle(member),
                onRequestRemove: onRequestRemove,
                onRetry: {},
                onDismissFailure: {}
            )
        }
    }
}

/// Observes the holder directly: a nested `ObservableObject` does not forward its changes to the
/// screen's view model, so the row subscribes itself.
///
/// The error haptic and the VoiceOver announcement fire on the change into `.failure`, not on
/// every redraw, so a failed row scrolled back into view does not repeat them.
private struct ObservedGamblerRow: View {
    @ObservedObject var holder: GamblerRowHolder
    let onRequestRemove: (PoolMemberModel) -> Void

    var body: some View {
        RemovableGamblerRow(
            state: holder.removal,
            onRequestRemove: onRequestRemove,
            onRetry: holder.retry,
            onDismissFailure: holder.dismissFailure
        )
        .onChange(of: holder.removal.isFailure()) { isFailure in
            guard isFailure else { return }
            announceFailure(of: holder.member)
        }
    }

    private func announceFailure(of member: PoolMemberModel) {
        UINotificationFeedbackGenerator().notificationOccurred(.error)
        UIAccessibility.post(
            notification: .announcement,
            argument: String(localized: .removeGamblerFailureAnnouncement("@\(member.gamblerUsername)"))
        )
    }
}

/// The row inside the swipe container: dragging it from the trailing edge past 56 pt and releasing
/// asks to remove the gambler, as does its VoiceOver "Remove" action. Neither is offered while the
/// removal runs.
private struct RemovableGamblerRow: View {
    let state: MutationState<PoolMemberModel>
    let onRequestRemove: (PoolMemberModel) -> Void
    let onRetry: () -> Void
    let onDismissFailure: () -> Void

    private var member: PoolMemberModel { state.activeValue() }
    private var isRemoving: Bool { state.isMutating() || state.isMutated() }

    var body: some View {
        SwipeToDismissBox(
            threshold: .distance(gamblerSwipeThreshold),
            isEnabled: !isRemoving,
            onConfirm: requestRemove,
            background: { _, isPastThreshold in
                ManageGamblerSwipeBackground(isPastThreshold: isPastThreshold)
            },
            content: {
                ManageGamblerItem(
                    state: state,
                    onRequestRemove: isRemoving ? nil : requestRemove,
                    onRetry: onRetry,
                    onDismissFailure: onDismissFailure
                )
                // The row is opaque at rest so the swipe background shows only where it is dragged.
                .background(Color(uiColor: .systemBackground))
            }
        )
    }

    private func requestRemove() {
        guard !isRemoving else { return }
        onRequestRemove(member)
    }
}

/// The background a dragged row reveals: the error fill with the person-remove glyph and "Remove",
/// both in `onError`. The glyph grows once releasing would ask to remove the gambler. The row's own
/// "Remove" action is what VoiceOver offers, so the background is hidden from it.
struct ManageGamblerSwipeBackground: View {
    let isPastThreshold: Bool

    @ScaledMetric(relativeTo: .caption) private var glyphSize: CGFloat = 24

    var body: some View {
        ZStack(alignment: .trailing) {
            Color(sharedResource: .error)

            VStack(spacing: swipeLabelSpacing) {
                Image(sharedResource: .personRemove)
                    .resizable()
                    .frame(width: glyphSize, height: glyphSize)
                    .scaleEffect(isPastThreshold ? pastThresholdGlyphScale : 1)
                    .animation(.easeOut(duration: 0.1), value: isPastThreshold)

                Text(.removeGamblerAction)
                    .font(.caption.weight(.semibold))
                    .lineLimit(1)
                    .fixedSize()
            }
            .foregroundStyle(Color(sharedResource: .onError))
            .padding(.trailing, rowHorizontalPadding)
        }
        .accessibilityHidden(true)
    }
}

/// The drag distance that confirms a swipe, whatever the row's width.
let gamblerSwipeThreshold: CGFloat = 56
private let pastThresholdGlyphScale: CGFloat = 1.22
private let swipeLabelSpacing: CGFloat = 2

#Preview("Swipe background") {
    VStack(spacing: 0) {
        ManageGamblerSwipeBackground(isPastThreshold: false)
            .frame(height: 64)
        ManageGamblerSwipeBackground(isPastThreshold: true)
            .frame(height: 64)
    }
}

#Preview("Swipe background, dark") {
    ManageGamblerSwipeBackground(isPastThreshold: true)
        .frame(height: 64)
        .preferredColorScheme(.dark)
}
