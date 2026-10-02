import SwiftUI
import UI
import Account
import ViewingState

/// One Manage gamblers row's content. Loading slots render this same component from
/// `poolMemberPlaceholderModel()` with `isPlaceholder: true`: native redaction under the shared
/// `LoadingPlaceholderPulse` conceals the avatar and text, the avatar draws no filler identity,
/// and the content ignores touches and stays out of the accessibility tree.
struct ManageGamblerItem: View {
    let state: MutationState<PoolMemberModel>
    let isPlaceholder: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    init(state: MutationState<PoolMemberModel>, isPlaceholder: Bool = false) {
        self.state = state
        self.isPlaceholder = isPlaceholder
    }

    private var member: PoolMemberModel { state.activeValue() }
    private var isDeleting: Bool { state.isMutating() || state.isMutated() }

    var body: some View {
        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { content }
            } else {
                content
                    .opacity(isDeleting ? 0.55 : 1)
                    .animation(.default, value: isDeleting)
            }
        }
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
    }

    private var content: some View {
        HStack(spacing: boxSpacing.medium) {
            avatar
                .frame(width: avatarSize, height: avatarSize)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: boxSpacing.small) {
                Text(member.gamblerUsername)
                    .fontWeight(.medium)
                    .loadedForeground(
                        member.isOwner ? Color(sharedResource: .onPrimaryContainter) : Color.primary,
                        isPlaceholder: isPlaceholder
                    )
                    .lineLimit(1)
                    .truncationMode(.tail)

                Text(member.gamblerEmail)
                    .font(.subheadline)
                    .loadedForeground(
                        member.isOwner ? Color(sharedResource: .onPrimaryContainter).opacity(0.7) : Color.secondary,
                        isPlaceholder: isPlaceholder
                    )
                    .lineLimit(1)
                    .truncationMode(.tail)
            }

            Spacer(minLength: 0)
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
        } else if isDeleting {
            ZStack {
                Circle().fill(Color(sharedResource: .surfaceVariant))
                BallSpinner()
                    .frame(width: avatarSize * 0.6, height: avatarSize * 0.6)
            }
            .transition(.opacity)
        } else {
            EmailAvatar(email: member.gamblerEmail)
                .transition(.opacity)
        }
    }
}

private let avatarSize: CGFloat = 40

#Preview("Default") {
    ManageGamblerItem(
        state: .idle(
            PoolMemberModel(
                gamblerId: "1",
                gamblerUsername: "danielsanto",
                gamblerEmail: "daniel@example.com"
            )
        )
    )
    .padding()
}

#Preview("Deleting") {
    ManageGamblerItem(
        state: .mutating(
            PoolMemberModel(
                gamblerId: "1",
                gamblerUsername: "danielsanto",
                gamblerEmail: "daniel@example.com"
            )
        )
    )
    .padding()
}

#Preview("Placeholder") {
    ManageGamblerItem(state: .idle(poolMemberPlaceholderModel()), isPlaceholder: true)
        .padding()
}
