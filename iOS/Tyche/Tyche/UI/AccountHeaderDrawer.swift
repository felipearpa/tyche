import SwiftUI

struct AccountHeaderDrawer: View {
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            ZStack {
                Circle()
                    .stroke(Color.accentColor, lineWidth: AVATAR_RING_WIDTH)
                    .frame(width: AVATAR_SIZE, height: AVATAR_SIZE)

                Image(.filledPerson)
                    .resizable()
                    .scaledToFit()
                    .frame(width: AVATAR_ICON_SIZE, height: AVATAR_ICON_SIZE)
                    .foregroundStyle(Color.secondary)
            }

            Text(email)
                .font(.headline)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .truncationMode(.tail)

            Text(String(.connectedAccountText))
                .font(.caption)
                .foregroundStyle(Color.secondary)

            RoundedRectangle(cornerRadius: ACCENT_LINE_HEIGHT / 2)
                .fill(Color.accentColor)
                .frame(width: ACCENT_LINE_WIDTH, height: ACCENT_LINE_HEIGHT)
        }
    }
}

private let AVATAR_SIZE: CGFloat = 96
private let AVATAR_ICON_SIZE: CGFloat = 56
private let AVATAR_RING_WIDTH: CGFloat = 3
private let ACCENT_LINE_WIDTH: CGFloat = 48
private let ACCENT_LINE_HEIGHT: CGFloat = 3
