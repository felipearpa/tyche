import SwiftUI

struct AccountHeaderDrawer: View {
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            EmailAvatar(email: email)
                .frame(width: AVATAR_SIZE, height: AVATAR_SIZE)
                .clipShape(Circle())

            Text(email)
                .font(.headline)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .truncationMode(.tail)
        }
    }
}

private let AVATAR_SIZE: CGFloat = 48

#Preview {
    AccountHeaderDrawer(email: "felipearpa@tyche.com")
}
