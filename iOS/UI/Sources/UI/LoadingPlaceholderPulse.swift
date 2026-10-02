import SwiftUI

/// Fortuna's shared loading-placeholder pulse: the timing, curve, and appearance endpoints that
/// every production component reads when it renders itself with `isPlaceholder == true`.
///
/// Components own their placeholder rendering; screens never pass an effect. A component conceals
/// its content regions with native `.redacted(reason: .placeholder)`, sets the redacted content's
/// foreground style to `fillColor` at `opacity(elapsed:)`, and leaves structural fills (row
/// canvases, rank tiles, separators) alone so they do not pulse.
///
/// Native redaction paints each redacted text or image as a block of its foreground style at 16%
/// of that style's opacity (measured with `ImageRenderer` on iOS 18.1 and 26.5), so the opacities
/// below scale the foreground style, not the final fill. Light appearance composites to black at
/// 9.6% → 16% → 9.6% with a 12.8% static midpoint; dark to white at 10.7% → 16% → 10.7% with a
/// 13.3% static midpoint. These keep the dim-to-bright ratios of the design preset (12:20 and
/// 16:24); 16% is the highest fill native redaction can produce.
public struct LoadingPlaceholderPulse: Equatable, Sendable {
    /// Foreground opacity at the dimmest point of the cycle.
    public let dimOpacity: Double
    /// Foreground opacity at the brightest point of the cycle.
    public let brightOpacity: Double
    /// Foreground opacity shown without animation when Reduce Motion is on.
    public let staticOpacity: Double

    /// Each fade direction at normal speed; the full cycle is two legs with no endpoint pause.
    public static let legDuration: TimeInterval = 0.9
    public static let cycleDuration: TimeInterval = legDuration * 2

    public static let light = LoadingPlaceholderPulse(
        dimOpacity: 0.6,
        brightOpacity: 1.0,
        staticOpacity: 0.8
    )

    public static let dark = LoadingPlaceholderPulse(
        dimOpacity: 2.0 / 3.0,
        brightOpacity: 1.0,
        staticOpacity: 5.0 / 6.0
    )

    public static func forColorScheme(_ colorScheme: ColorScheme) -> LoadingPlaceholderPulse {
        colorScheme == .dark ? dark : light
    }

    /// Neutral fill for redacted content: black in light appearance, white in dark. Using one
    /// neutral style keeps semantic colors (trend green/red, rank-tile text) from tinting masks.
    public var fillColor: Color { .primary }

    /// The foreground opacity `elapsed` seconds after the pulse started. The cycle starts dim,
    /// eases in and out to bright over one leg, and returns over the next.
    public func opacity(elapsed: TimeInterval) -> Double {
        let cycle = Self.cycleDuration
        let position = (elapsed.truncatingRemainder(dividingBy: cycle) + cycle)
            .truncatingRemainder(dividingBy: cycle) / Self.legDuration
        let leg = position <= 1 ? position : 2 - position
        let eased = leg * leg * (3 - 2 * leg)
        return dimOpacity + (brightOpacity - dimOpacity) * eased
    }
}
