import SwiftUI

public struct DrawerButtonRow<Icon: View>: View {
    let icon: Icon
    let title: String
    var tint: Color = .primary
    let action: () -> Void

    public init(
        @ViewBuilder icon: () -> Icon,
        title: String,
        tint: Color = .primary,
        action: @escaping () -> Void
    ) {
        self.icon = icon()
        self.title = title
        self.tint = tint
        self.action = action
    }

    @Environment(\.boxSpacing) private var boxSpacing

    public var body: some View {
        Button(action: action) {
            HStack(spacing: boxSpacing.medium) {
                icon
                    .frame(width: MENU_ICON_SIZE, height: MENU_ICON_SIZE)
                    .foregroundStyle(tint)

                Text(title)
                    .foregroundStyle(tint)

                Spacer()
            }
            .padding(.vertical, boxSpacing.medium)
        }
    }
}

private let MENU_ICON_SIZE: CGFloat = 24
