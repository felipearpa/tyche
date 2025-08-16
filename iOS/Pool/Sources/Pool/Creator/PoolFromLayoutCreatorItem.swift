import SwiftUI
import UI

struct PoolFromLayoutCreatorItem: View {
    let poolLayout: PoolLayoutModel
    let isSelected: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let backgroundColor = isSelected ? Color(sharedResource: .primaryContainer) : Color(sharedResource: .surfaceVariant)
        let foregroundColor = isSelected ? Color(sharedResource: .onPrimaryContainter) : Color(sharedResource: .onSurfaceVariant)

        CardView(backgroundColor: backgroundColor, foregroundColor: foregroundColor) {
            HStack(spacing: boxSpacing.medium) {
                VStack(alignment: .leading, spacing: boxSpacing.small) {
                    Text(poolLayout.name)
                        .font(.title3)
                        .foregroundColor(foregroundColor)

                    Text(String(
                        format: String(.startingFromDateText),
                        poolLayout.startDateTime.toShortDateString()
                    ))
                    .font(.footnote)
                    .foregroundColor(foregroundColor)
                }

                Spacer()

                Image(sharedResource: .arrowForwardIos)
                    .foregroundColor(foregroundColor)
            }
            .padding(boxSpacing.medium)
        }
    }
}

private struct CardView<Content: View>: View {
    let backgroundColor: Color
    let foregroundColor: Color
    let content: () -> Content

    var body: some View {
        content()
            .background(backgroundColor)
            .foregroundColor(foregroundColor)
            .cornerRadius(12)
    }
}

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
