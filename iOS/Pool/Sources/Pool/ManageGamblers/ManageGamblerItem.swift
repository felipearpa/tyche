import SwiftUI
import UI
import Account
import ViewingState

struct ManageGamblerItem: View {
    let state: MutationState<PoolMemberModel>

    @Environment(\.boxSpacing) private var boxSpacing

    private var member: PoolMemberModel { state.activeValue() }
    private var isDeleting: Bool { state.isMutating() || state.isMutated() }

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            avatar
                .frame(width: avatarSize, height: avatarSize)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: boxSpacing.small) {
                Text(member.gamblerUsername)
                    .fontWeight(.medium)
                    .foregroundStyle(member.isOwner ? Color(sharedResource: .onPrimaryContainter) : Color.primary)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Text(member.gamblerEmail)
                    .font(.subheadline)
                    .foregroundStyle(member.isOwner ? Color(sharedResource: .onPrimaryContainter).opacity(0.7) : Color.secondary)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }

            Spacer(minLength: 0)
        }
        .opacity(isDeleting ? 0.55 : 1)
        .animation(.default, value: isDeleting)
    }

    @ViewBuilder
    private var avatar: some View {
        if isDeleting {
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
