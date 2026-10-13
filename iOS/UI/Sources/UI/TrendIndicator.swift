import Core
import SwiftUI
import UIKit

/// How a `TrendIndicator` sits in its surface.
public enum TrendIndicatorStyle: Sendable {
    /// Symbol and value on the surface itself, centered, as in the leaderboard and My pools rank
    /// rails.
    case plain
    /// A neutral capsule around the symbol and value, trailing-aligned, as in History and Timeline
    /// group headers.
    case badge
}

/// The shared presentation of a gambler's rank movement. It owns the direction, symbol, semantic
/// color, and absolute-value formatting of a numeric movement, the `1 of 2 computed` qualifier of a
/// partial kickoff group, and the explicit pending and unavailable labels, so no surface repeats
/// those decisions. Pending and unavailable never use the steady symbol, which means a confirmed
/// zero.
///
/// The indicator has no accessibility semantics of its own: the enclosing row or header speaks
/// `RankMovement.accessibilityText` as part of its single announcement.
public struct TrendIndicator: View {
    let movement: RankMovement
    let textStyle: Font.TextStyle
    let style: TrendIndicatorStyle
    let isPlaceholder: Bool

    /// - Parameter isPlaceholder: When `true`, the indicator drops its semantic colors and
    ///   inherits the enclosing placeholder's neutral foreground, so a redacting parent's
    ///   pulse is not tinted green, red, or gray. The parent owns redaction and the pulse.
    public init(
        movement: RankMovement,
        textStyle: Font.TextStyle = .footnote,
        style: TrendIndicatorStyle = .plain,
        isPlaceholder: Bool = false
    ) {
        self.movement = movement
        self.textStyle = textStyle
        self.style = style
        self.isPlaceholder = isPlaceholder
    }

    public var body: some View {
        VStack(alignment: horizontalAlignment, spacing: qualifierSpacing) {
            symbolLine
            if let progress = partialProgress {
                Text(.trendProgressLabel(progress.computedMatches, progress.totalMatches))
                    .font(.system(qualifierTextStyle))
                    .multilineTextAlignment(textAlignment)
                    .fixedSize(horizontal: false, vertical: true)
                    .trendForeground(secondaryText, isPlaceholder: isPlaceholder)
            }
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private var symbolLine: some View {
        switch style {
        case .plain:
            symbolContent
        case .badge:
            symbolContent
                .padding(.horizontal, badgeHorizontalPadding)
                .padding(.vertical, badgeVerticalPadding)
                .background(Capsule().fill(Color(uiColor: .secondarySystemFill)))
        }
    }

    @ViewBuilder
    private var symbolContent: some View {
        switch movement {
        case .moved(let places, _) where places > 0:
            DirectionalTrend(image: .arrowUpward, places: places, textStyle: textStyle, style: style)
                .trendForeground(Color(.positive), isPlaceholder: isPlaceholder)
        case .moved(let places, _) where places < 0:
            DirectionalTrend(image: .arrowDownward, places: places, textStyle: textStyle, style: style)
                .trendForeground(Color(.negative), isPlaceholder: isPlaceholder)
        case .moved:
            TrendSymbol(image: .horizontalRule, textStyle: textStyle)
                .trendForeground(Color(.neutral), isPlaceholder: isPlaceholder)
        case .pending:
            stateLabel(.trendPendingLabel)
        case .unavailable:
            stateLabel(.trendUnavailableLabel)
        }
    }

    private func stateLabel(_ resource: LocalizedStringResource) -> some View {
        Text(resource)
            .font(.system(textStyle))
            .multilineTextAlignment(textAlignment)
            .fixedSize(horizontal: false, vertical: true)
            .trendForeground(secondaryText, isPlaceholder: isPlaceholder)
    }

    private var partialProgress: KickoffGroupProgress? {
        if case .moved(_, let progress) = movement { return progress }
        return nil
    }

    private var horizontalAlignment: HorizontalAlignment {
        style == .badge ? .trailing : .center
    }

    private var textAlignment: TextAlignment {
        style == .badge ? .trailing : .center
    }

    /// The qualifier stays one step below the value it qualifies.
    private var qualifierTextStyle: Font.TextStyle {
        style == .badge ? .caption : .caption2
    }
}

public extension RankMovement {
    /// Whether `TrendIndicator` shows text beyond a symbol and number: a partial group's qualifier
    /// or the pending or unavailable label. Rails reserve room to wrap it.
    var hasLabel: Bool {
        switch self {
        case .pending, .unavailable: true
        case .moved(_, let progress): progress != nil
        }
    }

    /// The spoken movement for a row or header announcement: the direction with its place count
    /// in full words, then the computed count of a partial group; or the pending or unavailable
    /// state.
    var accessibilityText: String {
        switch self {
        case .pending:
            return String(localized: .trendPendingAccessibility)
        case .unavailable:
            return String(localized: .trendUnavailableAccessibility)
        case .moved(let places, let progress):
            let direction = Self.directionAccessibilityText(places)
            guard let progress else { return direction }
            let qualifier = String(
                localized: .trendProgressAccessibility(progress.computedMatches, matches: progress.totalMatches)
            )
            return "\(direction), \(qualifier)"
        }
    }

    private static func directionAccessibilityText(_ places: Int) -> String {
        if places > 0 {
            return String(localized: .trendUpAccessibility(places))
        }
        if places < 0 {
            return String(localized: .trendDownAccessibility(abs(places)))
        }
        return String(localized: .trendUnchangedAccessibility)
    }
}

private extension View {
    @ViewBuilder
    func trendForeground<S: ShapeStyle>(_ semanticStyle: S, isPlaceholder: Bool) -> some View {
        if isPlaceholder {
            self
        } else {
            foregroundStyle(semanticStyle)
        }
    }
}

/// An arrow with the absolute number of places moved.
private struct DirectionalTrend: View {
    let image: ImageResource
    let places: Int
    let textStyle: Font.TextStyle
    let style: TrendIndicatorStyle

    var body: some View {
        HStack(spacing: style == .badge ? badgeSymbolSpacing : 0) {
            TrendSymbol(image: image, textStyle: textStyle)
            Text(String(abs(places)))
                .font(.system(textStyle).weight(style == .badge ? .semibold : .regular))
                .monospacedDigit()
        }
    }
}

private struct TrendSymbol: View {
    let image: ImageResource
    @ScaledMetric private var iconSize: CGFloat

    init(image: ImageResource, textStyle: Font.TextStyle) {
        self.image = image
        _iconSize = ScaledMetric(wrappedValue: baseIconSize(for: textStyle), relativeTo: textStyle)
    }

    var body: some View {
        Image(image)
            .resizable()
            .frame(width: iconSize, height: iconSize)
    }
}

/// The qualifier and state labels: 5.9:1 on the light canvas and 7.7:1 on the dark one, 5.6:1 and
/// 6.4:1 on the badge fill (History's measured secondary text).
private let secondaryText = Color(.onSurface).opacity(0.7)
private let qualifierSpacing: CGFloat = 2
private let badgeHorizontalPadding: CGFloat = 12
private let badgeVerticalPadding: CGFloat = 6
private let badgeSymbolSpacing: CGFloat = 4

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

#Preview("Plain") {
    VStack(spacing: 16) {
        TrendIndicator(movement: .moved(places: 3, partialProgress: nil), textStyle: .caption2)
        TrendIndicator(movement: .moved(places: -1, partialProgress: nil), textStyle: .caption2)
        TrendIndicator(movement: .moved(places: 0, partialProgress: nil), textStyle: .caption2)
        TrendIndicator(
            movement: .moved(places: 3, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2)),
            textStyle: .caption2
        )
        TrendIndicator(movement: .pending, textStyle: .caption2)
        TrendIndicator(movement: .unavailable, textStyle: .caption2)
    }
    .frame(width: 64)
}

#Preview("Badge") {
    VStack(alignment: .trailing, spacing: 16) {
        TrendIndicator(movement: .moved(places: 3, partialProgress: nil), textStyle: .body, style: .badge)
        TrendIndicator(movement: .moved(places: -1, partialProgress: nil), textStyle: .body, style: .badge)
        TrendIndicator(movement: .moved(places: 0, partialProgress: nil), textStyle: .body, style: .badge)
        TrendIndicator(
            movement: .moved(places: 2, partialProgress: KickoffGroupProgress(computedMatches: 1, totalMatches: 2)),
            textStyle: .body,
            style: .badge
        )
        TrendIndicator(movement: .pending, textStyle: .body, style: .badge)
        TrendIndicator(movement: .unavailable, textStyle: .body, style: .badge)
    }
    .padding()
}
