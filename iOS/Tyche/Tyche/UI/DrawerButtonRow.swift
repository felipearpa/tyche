import SwiftUI
import UI

/// A drawer action: an icon centered in the drawer's leading column, so labels align with the
/// account name above, plus an optional trailing accessory. The row spans the drawer's width,
/// grows with Dynamic Type, keeps at least a native touch target's height, and uses the shared
/// quiet pressed state.
public struct DrawerButtonRow<Accessory: View>: View {
    let icon: Image
    let title: String
    let tint: Color
    let accessory: Accessory
    let action: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var iconSize: CGFloat = 24

    public init(
        icon: Image,
        title: String,
        tint: Color = .primary,
        @ViewBuilder accessory: () -> Accessory,
        action: @escaping () -> Void
    ) {
        self.icon = icon
        self.title = title
        self.tint = tint
        self.accessory = accessory()
        self.action = action
    }

    /// Icons grow with Dynamic Type up to half again their size, so at accessibility sizes the
    /// label keeps the width it needs instead of wrapping mid-word beside an oversized icon.
    private var cappedIconSize: CGFloat {
        min(iconSize, maximumIconSize)
    }

    /// At accessibility sizes the accessory moves below the label, so the label gets the full
    /// width and wraps on word boundaries instead of mid-word.
    private var labelLayout: AnyLayout {
        dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: boxSpacing.small))
            : AnyLayout(HStackLayout(spacing: boxSpacing.medium))
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: boxSpacing.medium) {
                icon
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: cappedIconSize, height: cappedIconSize)
                    .frame(minWidth: drawerLeadingColumnWidth)
                    .accessibilityHidden(true)

                labelLayout {
                    Text(title)
                        .font(.body)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    accessory
                }
            }
            .foregroundStyle(tint)
            .padding(.horizontal, boxSpacing.large)
            .padding(.vertical, boxSpacing.medium)
            .frame(minHeight: minimumRowHeight)
            // The row styles its own colors, so it shows the disabled state itself.
            .opacity(isEnabled ? 1 : disabledOpacity)
        }
        .buttonStyle(InteractiveRowButtonStyle())
    }
}

public extension DrawerButtonRow where Accessory == EmptyView {
    init(
        icon: Image,
        title: String,
        tint: Color = .primary,
        action: @escaping () -> Void
    ) {
        self.init(icon: icon, title: title, tint: tint, accessory: { EmptyView() }, action: action)
    }
}

/// The width of the drawer's leading column: the account avatar, and the icons centered below it.
let drawerLeadingColumnWidth: CGFloat = 48

private let minimumRowHeight: CGFloat = 44
private let maximumIconSize: CGFloat = 36
private let disabledOpacity: CGFloat = 0.5
