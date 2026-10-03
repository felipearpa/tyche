import SwiftUI
import UI

/// Placeholder rendering for this module's production components when they receive
/// `isPlaceholder: true`. Native redaction conceals every content region, and the redacted
/// content's neutral foreground follows the shared `LoadingPlaceholderPulse`. Structural fills
/// (row canvases, section headers) are not foreground content, so they keep their production
/// colors. Reduce Motion, read live from the environment, holds the static midpoint instead.
///
/// Module-internal on purpose: components apply it to their own content; screens never do.
/// The timeline stops when the component leaves placeholder presentation or the hierarchy.
struct PulsingPlaceholderContent<Content: View>: View {
    @ViewBuilder let content: Content

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorScheme) private var colorScheme
    @State private var pulseStart = Date()

    var body: some View {
        let pulse = LoadingPlaceholderPulse.forColorScheme(colorScheme)

        if reduceMotion {
            redacted(opacity: pulse.staticOpacity, pulse: pulse)
        } else {
            TimelineView(.animation) { timeline in
                redacted(
                    opacity: pulse.opacity(elapsed: timeline.date.timeIntervalSince(pulseStart)),
                    pulse: pulse
                )
            }
        }
    }

    private func redacted(opacity: Double, pulse: LoadingPlaceholderPulse) -> some View {
        content
            .redacted(reason: .placeholder)
            .foregroundStyle(pulse.fillColor.opacity(opacity))
    }
}


extension View {
    /// Applies a component's own foreground only to loaded content. Placeholders keep the
    /// enclosing pulse's neutral fill, so semantic or container colors never tint their masks.
    @ViewBuilder
    func loadedForeground<S: ShapeStyle>(_ style: S, isPlaceholder: Bool) -> some View {
        if isPlaceholder {
            self
        } else {
            foregroundStyle(style)
        }
    }
}
