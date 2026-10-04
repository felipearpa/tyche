import Account
import SwiftUI
import UI

/// The selected gambler's identity at the top of their Timeline: the shared account avatar and
/// their display name. The name comes from navigation, so it is always real content; the avatar
/// loads through the shared avatar store and falls back to the shared letter avatar. The avatar
/// is decorative next to the name, which is the screen's heading.
struct TimelineGamblerHeading: View {
    let gamblerId: String
    let gamblerUsername: String

    @ScaledMetric(relativeTo: .title) private var avatarSize: CGFloat = 44

    var body: some View {
        HStack(alignment: .center, spacing: avatarNameSpacing) {
            AccountAvatar(
                accountId: gamblerId,
                // The leaderboard's fallback, so the letter avatar keeps the same letter and color.
                fallback: AccountAvatarFallback(identity: gamblerUsername, colorKey: gamblerUsername)
            )
            .frame(width: cappedAvatarSize, height: cappedAvatarSize)
            .clipShape(Circle())
            .accessibilityHidden(true)

            Text(verbatim: gamblerUsername)
                .font(.title.weight(.bold))
                .foregroundStyle(HistoryStyle.primaryText)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
        }
    }

    /// The avatar grows with text size up to a cap, leaving the width to the name.
    private var cappedAvatarSize: CGFloat {
        min(avatarSize, maximumAvatarSize)
    }
}

private let avatarNameSpacing: CGFloat = 12
private let maximumAvatarSize: CGFloat = 72

#Preview {
    VStack(alignment: .leading, spacing: 24) {
        TimelineGamblerHeading(gamblerId: "", gamblerUsername: "El mono")
        TimelineGamblerHeading(gamblerId: "", gamblerUsername: "giraldocano517@gmail.com")
        TimelineGamblerHeading(gamblerId: "", gamblerUsername: "Juan Pablo Rojas de la Santísima Trinidad")
    }
    .historyHorizontalGutter()
}
