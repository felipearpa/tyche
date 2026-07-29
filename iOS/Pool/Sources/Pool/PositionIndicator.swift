import SwiftUI

public struct PostionIndicator: View {
    private let position: Int?
    private let shouldUsePrimeryColor: Bool
    private let size: CGFloat
    private let cornerRadius: CGFloat
    private let backgroundColor: Color?
    private let backgroundOverlayColor: Color?
    private let foregroundColor: Color?
    private let font: Font?

    public init(
        position: Int?,
        shouldUsePrimeryColor: Bool,
        size: CGFloat = 32,
        cornerRadius: CGFloat = 8,
        backgroundColor: Color? = nil,
        backgroundOverlayColor: Color? = nil,
        foregroundColor: Color? = nil,
        font: Font? = nil
    ) {
        self.position = position
        self.shouldUsePrimeryColor = shouldUsePrimeryColor
        self.size = size
        self.cornerRadius = cornerRadius
        self.backgroundColor = backgroundColor
        self.backgroundOverlayColor = backgroundOverlayColor
        self.foregroundColor = foregroundColor
        self.font = font
    }

    public var body: some View {
        ZStack {
            Text(position.map(String.init) ?? "—")
                .font(font)
                .monospacedDigit()
        }
        .frame(width: size, height: size)
        .background {
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(resolvedBackgroundColor)
                .overlay {
                    if let backgroundOverlayColor {
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .fill(backgroundOverlayColor)
                    }
                }
        }
        .foregroundStyle(resolvedForegroundColor)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
    }

    private var resolvedBackgroundColor: Color {
        backgroundColor
            ?? (shouldUsePrimeryColor
                ? Color(sharedResource: .primaryContainer)
                : Color(sharedResource: .secondaryContainer))
    }

    private var resolvedForegroundColor: Color {
        foregroundColor
            ?? (shouldUsePrimeryColor
                ? Color(sharedResource: .onPrimaryContainter)
                : Color(sharedResource: .onSecondaryContainer))
    }
}

#Preview("Default signed in") {
    PostionIndicator(position: 1, shouldUsePrimeryColor: true)
}

#Preview("Default not signed in") {
    PostionIndicator(position: 1, shouldUsePrimeryColor: false)
}

#Preview("Leaderboard") {
    PostionIndicator(
        position: 3,
        shouldUsePrimeryColor: false,
        size: 44,
        cornerRadius: 10,
        backgroundColor: Color(sharedResource: .surfaceVariant),
        foregroundColor: Color(sharedResource: .onSurfaceVariant),
        font: .title3.weight(.semibold)
    )
}

#Preview("Missing position") {
    PostionIndicator(position: nil, shouldUsePrimeryColor: false)
}
