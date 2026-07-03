import SwiftUI
import UIKit

public struct TrendIndicator: View {
    let difference: Int
    let textStyle: Font.TextStyle

    public init(difference: Int, textStyle: Font.TextStyle = .footnote) {
        self.difference = difference
        self.textStyle = textStyle
    }

    public var body: some View {
        if difference > 0 {
            UpTrendIndicator(progress: difference, textStyle: textStyle)
        } else if difference < 0 {
            DownTrendIndicator(progress: difference, textStyle: textStyle)
        } else {
            StableTrendIndicator(textStyle: textStyle)
        }
    }
}

private struct UpTrendIndicator: View {
    let progress: Int
    let textStyle: Font.TextStyle
    @ScaledMetric private var iconSize: CGFloat

    init(progress: Int, textStyle: Font.TextStyle) {
        self.progress = progress
        self.textStyle = textStyle
        _iconSize = ScaledMetric(wrappedValue: baseIconSize(for: textStyle), relativeTo: textStyle)
    }

    var body: some View {
        HStack(spacing: 0) {
            Image(.arrowUpward)
                .resizable()
                .frame(width: iconSize, height: iconSize)
            Text(String(abs(progress)))
                .font(.system(textStyle))
        }
        .foregroundStyle(Color(.positive))
    }
}

private struct DownTrendIndicator: View {
    let progress: Int
    let textStyle: Font.TextStyle
    @ScaledMetric private var iconSize: CGFloat

    init(progress: Int, textStyle: Font.TextStyle) {
        self.progress = progress
        self.textStyle = textStyle
        _iconSize = ScaledMetric(wrappedValue: baseIconSize(for: textStyle), relativeTo: textStyle)
    }

    var body: some View {
        HStack(spacing: 0) {
            Image(.arrowDownward)
                .resizable()
                .frame(width: iconSize, height: iconSize)
            Text(String(abs(progress)))
                .font(.system(textStyle))
        }
        .foregroundStyle(Color(.negative))
    }
}

private struct StableTrendIndicator: View {
    @ScaledMetric private var iconSize: CGFloat

    init(textStyle: Font.TextStyle) {
        _iconSize = ScaledMetric(wrappedValue: baseIconSize(for: textStyle), relativeTo: textStyle)
    }

    var body: some View {
        Image(.horizontalRule)
            .resizable()
            .frame(width: iconSize, height: iconSize)
            .foregroundStyle(Color(.neutral))
    }
}

// Icon sized proportionally to the text, mirroring the Android TrendIndicator.
private let iconScale: CGFloat = 1.5

private func baseIconSize(for textStyle: Font.TextStyle) -> CGFloat {
    let defaultTraits = UITraitCollection(preferredContentSizeCategory: .large)
    return UIFont.preferredFont(forTextStyle: textStyle.uiTextStyle, compatibleWith: defaultTraits)
        .pointSize * iconScale
}

private extension Font.TextStyle {
    var uiTextStyle: UIFont.TextStyle {
        switch self {
        case .largeTitle: return .largeTitle
        case .title: return .title1
        case .title2: return .title2
        case .title3: return .title3
        case .headline: return .headline
        case .subheadline: return .subheadline
        case .body: return .body
        case .callout: return .callout
        case .footnote: return .footnote
        case .caption: return .caption1
        case .caption2: return .caption2
        default: return .body
        }
    }
}

#Preview {
    TrendIndicator(difference: 0)
}

#Preview {
    TrendIndicator(difference: 1)
}

#Preview {
    TrendIndicator(difference: -1)
}
