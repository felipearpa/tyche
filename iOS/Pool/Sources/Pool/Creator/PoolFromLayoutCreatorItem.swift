import SwiftUI
import UI

/// A pool template card. Loading slots render this same component from
/// `poolLayoutFakeModel()` with `isPlaceholder: true`: the card keeps its production surface,
/// its content is concealed by native redaction under the shared `LoadingPlaceholderPulse`,
/// and it ignores touches and stays out of the accessibility tree.
struct PoolFromLayoutCreatorItem: View {
    let poolLayout: PoolLayoutModel
    let isSelected: Bool
    let isPlaceholder: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    init(poolLayout: PoolLayoutModel, isSelected: Bool, isPlaceholder: Bool = false) {
        self.poolLayout = poolLayout
        self.isSelected = isSelected
        self.isPlaceholder = isPlaceholder
    }

    var body: some View {
        let backgroundColor = isSelected ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .surfaceVariant)
        let foregroundColor = isSelected ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSurfaceVariant)

        Group {
            if isPlaceholder {
                PulsingPlaceholderContent { cardContent }
            } else {
                cardContent.foregroundColor(foregroundColor)
            }
        }
        .background(backgroundColor)
        .cornerRadius(cardCornerRadius)
        .allowsHitTesting(!isPlaceholder)
        .accessibilityHidden(isPlaceholder)
    }

    private var cardContent: some View {
        HStack(spacing: boxSpacing.medium) {
            VStack(alignment: .leading, spacing: boxSpacing.small) {
                Text(poolLayout.name)
                    .font(.title3)

                Text(.startingFromDateText(poolLayout.startDateTime.toShortDateString()))
                    .font(.footnote)
            }

            Spacer()

            Image(sharedResource: .arrowForwardIos)
        }
        .padding(boxSpacing.medium)
    }
}

private let cardCornerRadius: CGFloat = 12

#Preview("Light not selected") {
    PoolFromLayoutCreatorItem(
        poolLayout: poolLayoutDummyModel(),
        isSelected: false,
    )
}

#Preview("Light selected") {
    PoolFromLayoutCreatorItem(
        poolLayout: poolLayoutDummyModel(),
        isSelected: true,
    )
}

#Preview("Dark not selected") {
    PoolFromLayoutCreatorItem(
        poolLayout: poolLayoutDummyModel(),
        isSelected: false,
    )
    .preferredColorScheme(.dark)
}

#Preview("Dark selected") {
    PoolFromLayoutCreatorItem(
        poolLayout: poolLayoutDummyModel(),
        isSelected: true,
    )
    .preferredColorScheme(.dark)
}

#Preview("Placeholder") {
    PoolFromLayoutCreatorItem(
        poolLayout: poolLayoutFakeModel(),
        isSelected: false,
        isPlaceholder: true
    )
}
