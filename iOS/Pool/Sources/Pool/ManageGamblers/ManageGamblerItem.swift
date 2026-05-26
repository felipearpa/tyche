import SwiftUI
import UI
import Account

struct ManageGamblerItem: View {
    let member: PoolMemberModel
    let isDeleting: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            avatar
                .frame(width: avatarSize, height: avatarSize)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: boxSpacing.small) {
                Text("@\(member.gamblerUsername)")
                    .fontWeight(.medium)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Group {
                    if isDeleting {
                        Text(.removingGamblerText)
                    } else {
                        Text(member.gamblerEmail)
                    }
                }
                .font(.subheadline)
                .foregroundStyle(Color.secondary)
                .lineLimit(1)
                .truncationMode(.tail)
            }

            Spacer(minLength: 0)
        }
        .opacity(isDeleting ? 0.55 : 1)
    }

    @ViewBuilder
    private var avatar: some View {
        if isDeleting {
            ZStack {
                Circle().fill(Color(sharedResource: .surfaceVariant))
                BallSpinner()
                    .frame(width: avatarSize * 0.6, height: avatarSize * 0.6)
            }
        } else {
            EmailAvatar(email: member.gamblerEmail)
        }
    }
}

private let avatarSize: CGFloat = 40

#Preview("Default") {
    ManageGamblerItem(
        member: PoolMemberModel(
            gamblerId: "1",
            gamblerUsername: "danielsanto",
            gamblerEmail: "daniel@example.com"
        ),
        isDeleting: false
    )
    .padding()
}

#Preview("Deleting") {
    ManageGamblerItem(
        member: PoolMemberModel(
            gamblerId: "1",
            gamblerUsername: "danielsanto",
            gamblerEmail: "daniel@example.com"
        ),
        isDeleting: true
    )
    .padding()
}
