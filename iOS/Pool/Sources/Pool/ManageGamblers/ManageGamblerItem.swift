import SwiftUI
import UI
import Account
import ViewingState

/// One Manage gamblers row: the member's avatar, username, and email in one of the row's states.
///
/// - The owner (the signed-in gambler on this screen) is drawn as the current user, with "You"
///   beside the username and an "Owner" capsule at the trailing edge.
/// - While a removal runs the content dims and the shared ball spinner replaces the avatar.
/// - A failed removal replaces the email with "Couldn't remove" and shows Retry (`onRetry`) and
///   dismiss (`onDismissFailure`) controls at the trailing edge.
///
/// The avatar, texts, and owner capsule form one VoiceOver element, which carries the "Remove"
/// action when `onRequestRemove` is given and the value "Removing" while a removal runs; the
/// failure controls stay separate elements.
///
/// Loading slots render this same component from `poolMemberPlaceholderModel()` with
/// `isPlaceholder: true`: native redaction under the shared `LoadingPlaceholderPulse` conceals the
/// avatar and text, the avatar draws no filler identity, and the content ignores touches and stays
/// out of the accessibility tree.
struct ManageGamblerItem: View {
    let state: MutationState<PoolMemberModel>
    let isPlaceholder: Bool
    let onRequestRemove: (() -> Void)?
    let onRetry: () -> Void
    let onDismissFailure: () -> Void

    @ScaledMetric(relativeTo: .subheadline) private var failureGlyphSize: CGFloat = 16
    @ScaledMetric(relativeTo: .body) private var closeGlyphSize: CGFloat = 20
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    init(
        state: MutationState<PoolMemberModel>,
        isPlaceholder: Bool = false,
        onRequestRemove: (() -> Void)?,
        onRetry: @escaping () -> Void,
        onDismissFailure: @escaping () -> Void
    ) {
        self.state = state
        self.isPlaceholder = isPlaceholder
        self.onRequestRemove = onRequestRemove
        self.onRetry = onRetry
        self.onDismissFailure = onDismissFailure
    }

    private var member: PoolMemberModel { state.activeValue() }
    private var isCurrentUser: Bool { member.isOwner && !isPlaceholder }
    private var isRemoving: Bool { state.isMutating() || state.isMutated() }
    private var isFailed: Bool { state.isFailure() && !isPlaceholder }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { content }
            } else {
                content
                    .opacity(isRemoving ? removingOpacity : 1)
                    .animation(.default, value: isRemoving)
            }
        }
        .padding(.horizontal, rowHorizontalPadding)
        .padding(.vertical, rowVerticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isCurrentUser ? Color(sharedResource: .currentUserContainer) : Color.clear)
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
    }

    /// At accessibility text sizes the failure controls move below the identity so the username
    /// and "Couldn't remove" keep their width.
    @ViewBuilder
    private var content: some View {
        if isFailed && dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: 0) {
                identity
                failureActions
            }
        } else {
            HStack(spacing: identitySpacing) {
                identity
                if isFailed {
                    failureActions
                }
            }
        }
    }

    private var identity: some View {
        HStack(spacing: identitySpacing) {
            avatar
                .frame(width: avatarSize, height: avatarSize)
                .clipShape(Circle())

            // At accessibility text sizes the owner capsule moves below the texts so the
            // username keeps the row's width.
            if isCurrentUser && dynamicTypeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: textSpacing) {
                    texts
                    ownerCapsule
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                texts
                    .frame(maxWidth: .infinity, alignment: .leading)
                if isCurrentUser {
                    ownerCapsule
                }
            }
        }
        .accessibilityElement(children: .combine)
        .modifier(RemovingAccessibilityValue(isRemoving: isRemoving && !isPlaceholder))
        .modifier(RemoveAccessibilityAction(onRequestRemove: onRequestRemove))
    }

    private var texts: some View {
        VStack(alignment: .leading, spacing: textSpacing) {
            usernameLine
            if isFailed {
                failureLine
            } else {
                Text(verbatim: member.gamblerEmail)
                    .font(.subheadline)
                    .loadedForeground(secondaryForeground, isPlaceholder: isPlaceholder)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
        }
    }

    private var usernameLine: some View {
        HStack(alignment: .firstTextBaseline, spacing: youLabelSpacing) {
            Text(verbatim: member.gamblerUsername)
                .fontWeight(.medium)
                .loadedForeground(primaryForeground, isPlaceholder: isPlaceholder)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? 2 : 1)
                .truncationMode(.tail)

            if isCurrentUser {
                Text(.manageGamblersYouLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(sharedResource: .currentUser))
                    .lineLimit(1)
                    .fixedSize()
            }
        }
    }

    /// "Couldn't remove" in place of the email. The announcement that names the gambler is posted
    /// by the row's state view when the failure arrives.
    private var failureLine: some View {
        HStack(spacing: failureGlyphSpacing) {
            Image(sharedResource: .error)
                .resizable()
                .frame(width: failureGlyphSize, height: failureGlyphSize)
                .accessibilityHidden(true)
            Text(.removeGamblerFailureText)
                .font(.subheadline)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(Color(sharedResource: .error))
    }

    private var ownerCapsule: some View {
        Text(.manageGamblersOwnerLabel)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Color(sharedResource: .currentUser))
            .lineLimit(1)
            .fixedSize()
            .padding(.horizontal, capsuleHorizontalPadding)
            .padding(.vertical, capsuleVerticalPadding)
            .background(Capsule().fill(Color(sharedResource: .currentUser).opacity(ownerCapsuleTintOpacity)))
    }

    private var failureActions: some View {
        HStack(spacing: 0) {
            Button(action: onRetry) {
                Text(.retryAction)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Color(sharedResource: .error))
                    .lineLimit(1)
                    .fixedSize()
                    .padding(.horizontal, capsuleHorizontalPadding)
                    .padding(.vertical, capsuleVerticalPadding)
                    .background(Capsule().fill(Color(sharedResource: .error).opacity(retryCapsuleTintOpacity)))
                    .frame(minWidth: minimumControlSize, minHeight: minimumControlSize)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(.retryRemoveGamblerAction("@\(member.gamblerUsername)")))

            Button(action: onDismissFailure) {
                Image(sharedResource: .close)
                    .resizable()
                    .frame(width: closeGlyphSize, height: closeGlyphSize)
                    .foregroundStyle(Color.secondary)
                    .frame(minWidth: minimumControlSize, minHeight: minimumControlSize)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(.dismissRemoveGamblerFailureAction("@\(member.gamblerUsername)")))
        }
    }

    @ViewBuilder
    private var avatar: some View {
        if isPlaceholder {
            AccountAvatar(
                accountId: "",
                fallback: AccountAvatarFallback(identity: ""),
                isPlaceholder: true
            )
        } else if isRemoving {
            ZStack {
                Circle().fill(Color(sharedResource: .surfaceVariant))
                BallSpinner()
                    .frame(width: avatarSize * 0.6, height: avatarSize * 0.6)
            }
            .transition(.opacity)
        } else {
            AccountAvatar(accountId: member.gamblerId, fallback: avatarFallback)
                .transition(.opacity)
        }
    }

    /// The letter avatar every other email-identified surface draws (the email's local part,
    /// colored by the whole email), in the current-user colors on the owner's row.
    private var avatarFallback: AccountAvatarFallback {
        AccountAvatarFallback(
            identity: member.gamblerEmail.split(separator: "@", maxSplits: 1, omittingEmptySubsequences: false)
                .first.map(String.init) ?? member.gamblerEmail,
            colorKey: member.gamblerEmail,
            backgroundColor: isCurrentUser ? Color(sharedResource: .currentUser) : nil,
            foregroundColor: isCurrentUser ? Color(sharedResource: .onCurrentUser) : nil
        )
    }

    private var primaryForeground: Color {
        isCurrentUser ? Color(sharedResource: .onCurrentUserContainer) : Color(sharedResource: .onSurface)
    }

    private var secondaryForeground: Color {
        isCurrentUser
            ? Color(sharedResource: .onCurrentUserContainer)
            : Color(sharedResource: .onSurface).opacity(secondaryTextOpacity)
    }
}

/// Describes the row to VoiceOver as "Removing" while its removal is in flight.
struct RemovingAccessibilityValue: ViewModifier {
    let isRemoving: Bool

    func body(content: Content) -> some View {
        if isRemoving {
            content.accessibilityValue(Text(.removingGamblerAccessibilityValue))
        } else {
            content
        }
    }
}

/// Offers VoiceOver's "Remove" custom action only when the row can be removed.
struct RemoveAccessibilityAction: ViewModifier {
    let onRequestRemove: (() -> Void)?

    func body(content: Content) -> some View {
        if let onRequestRemove {
            content.accessibilityAction(named: Text(.removeGamblerAction), onRequestRemove)
        } else {
            content
        }
    }
}

/// The row's horizontal gutter, shared with its separator and the screen's heading.
let rowHorizontalPadding: CGFloat = 16
private let rowVerticalPadding: CGFloat = 12
private let avatarSize: CGFloat = 40
private let identitySpacing: CGFloat = 12
private let textSpacing: CGFloat = 2
private let youLabelSpacing: CGFloat = 8
private let failureGlyphSpacing: CGFloat = 4
private let capsuleHorizontalPadding: CGFloat = 12
private let capsuleVerticalPadding: CGFloat = 6
private let minimumControlSize: CGFloat = 44
private let removingOpacity = 0.55
/// `onSurface` at 70 %: 5.9:1 on the light canvas and 7.7:1 on the dark one.
private let secondaryTextOpacity = 0.7
/// Light `currentUser` text keeps 4.5:1 on the capsule up to this tint over the current-user
/// container (4.55:1 at 14 %, 4.41:1 at 16 %); dark is 5.4:1.
private let ownerCapsuleTintOpacity = 0.14
/// `error` text on this tint of itself: 5.2:1 on the light canvas and 5.0:1 on the dark one.
private let retryCapsuleTintOpacity = 0.18

private let previewMember = PoolMemberModel(
    gamblerId: "1",
    gamblerUsername: "santiago",
    gamblerEmail: "santiago@example.com"
)

private let previewOwner = PoolMemberModel(
    gamblerId: "0",
    gamblerUsername: "felipearpa",
    gamblerEmail: "felipe@example.com",
    isOwner: true
)

private struct ManageGamblerItemStatesPreview: View {
    var body: some View {
        VStack(spacing: 0) {
            ManageGamblerItem(state: .idle(previewOwner), onRequestRemove: nil, onRetry: {}, onDismissFailure: {})
            Divider()
            ManageGamblerItem(state: .idle(previewMember), onRequestRemove: {}, onRetry: {}, onDismissFailure: {})
            Divider()
            ManageGamblerItem(state: .mutating(previewMember), onRequestRemove: nil, onRetry: {}, onDismissFailure: {})
            Divider()
            ManageGamblerItem(
                state: .failure(value: previewMember, error: CancellationError()),
                onRequestRemove: {},
                onRetry: {},
                onDismissFailure: {}
            )
            Divider()
            ManageGamblerItem(
                state: .idle(poolMemberPlaceholderModel()),
                isPlaceholder: true,
                onRequestRemove: nil,
                onRetry: {},
                onDismissFailure: {}
            )
        }
    }
}

#Preview("States, light") {
    ManageGamblerItemStatesPreview()
        .preferredColorScheme(.light)
}

#Preview("States, dark") {
    ManageGamblerItemStatesPreview()
        .preferredColorScheme(.dark)
}

#Preview("States, large text") {
    ScrollView {
        ManageGamblerItemStatesPreview()
    }
    .environment(\.dynamicTypeSize, .accessibility2)
}

#Preview("States, Spanish") {
    ManageGamblerItemStatesPreview()
        .environment(\.locale, Locale(identifier: "es-CO"))
}
