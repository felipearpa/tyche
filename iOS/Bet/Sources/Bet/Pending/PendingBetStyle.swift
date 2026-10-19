import SwiftUI
import UIKit
import UI

/// Colors and measurements of the Bets tab's match cards.
///
/// Measured contrast (WCAG) on the card (`secondarySystemBackground`, `#F2F2F7` light and
/// `#1C1C1E` dark):
/// - `secondaryText`: 5.7:1 light, 6.9:1 dark.
/// - `amber` text: 5.1:1 / 10.4:1 on the card and 4.8:1 / 7.7:1 on `amberFill`.
/// - `savedText` on `savedFill`: 4.8:1 / 7.9:1.
/// - `errorText` on `errorFill`: 4.53:1 light (`#B71C1C`) and 6.7:1 dark (`#EF9A9A`).
/// - `emptyOutline`, the dashed boundary of an empty score box: 3.1:1 / 4.2:1.
/// - `focusedOutline` (the accent green): 4.6:1 / 6.1:1.
/// - Stepper glyphs (`secondaryText`) on `stepperFill`: 5.4:1 light and 5.9:1 dark.
enum PendingBetStyle {
    static let cardFill = Color(uiColor: .secondarySystemBackground)
    static let controlFill = Color(uiColor: .tertiarySystemFill)
    /// The `+`/`−` steppers' fill, softer than the score boxes' `tertiarySystemFill` (12 % light,
    /// 24 % dark) so the boxes stay the visual focus: 1.09:1 against the card in light and 1.25:1
    /// in dark, as Android's steppers.
    static let stepperFill = Color(uiColor: UIColor { traits in
        UIColor(
            red: 118 / 255, green: 118 / 255, blue: 128 / 255,
            alpha: traits.userInterfaceStyle == .dark ? 0.195 : 0.08
        )
    })
    static let primaryText = Color(sharedResource: .onSurface)
    static let secondaryText = Color(sharedResource: .onSurface).opacity(0.7)
    static let emptyOutline = Color(sharedResource: .onSurface).opacity(0.5)
    static let focusedOutline = Color.accentColor

    /// The lock note, the Incomplete pill, and the missing score's box.
    static let amber = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0xFF / 255, green: 0xC1 / 255, blue: 0x07 / 255, alpha: 1)
            : UIColor(red: 0x8C / 255, green: 0x5E / 255, blue: 0x00 / 255, alpha: 1)
    })
    static let amberFill = Color(uiColor: UIColor(red: 0xFF / 255, green: 0xC1 / 255, blue: 0x07 / 255, alpha: 0.14))

    static let savedText = Color(sharedResource: .currentUser)
    static let savedFill = Color.accentColor.opacity(0.16)

    static let errorText = Color(sharedResource: .negative)
    static let errorFill = Color(sharedResource: .error).opacity(0.14)

    static let cardCornerRadius: CGFloat = 16
    static let cardHorizontalPadding: CGFloat = 16
    static let cardVerticalPadding: CGFloat = 14
    static let flagSize: CGFloat = 44
    static let scoreBoxSize: CGFloat = 52
    static let scoreBoxCornerRadius: CGFloat = 12
    static let scoreFont = Font.system(size: 34, weight: .bold).monospacedDigit()
    static let stepperWidth: CGFloat = 52
    static let stepperHeight: CGFloat = 44
    static let stepperGlyphSize: CGFloat = 24
    static let chipHeight: CGFloat = 36
    static let minimumTarget: CGFloat = 44
    static let pillHorizontalPadding: CGFloat = 10
    static let pillGlyphSize: CGFloat = 14
    static let spinnerSize: CGFloat = 20

    /// The one curve of every card change: the pill and spinner crossfade, score box colors, and
    /// rows entering or leaving below the scores with the card height that follows them.
    static let animation = Animation.easeInOut(duration: animationDuration)
    static let animationDuration: TimeInterval = 0.25

    /// The status pill's content swap within that change: the old content is gone within the first
    /// 40 % of it, and the new one fades in after that, so the two never overlap.
    static let contentFadeOut = Animation.easeOut(duration: animationDuration * 0.4)
    static let contentFadeIn = Animation.easeInOut(duration: animationDuration * 0.6)
        .delay(animationDuration * 0.4)

    /// Swaps one piece of content for another without overlap: the old fades out over the first
    /// 40 % of the change and the new fades in over the rest. Used by the status pill and the
    /// filter chip counts.
    static let contentSwap = AnyTransition.asymmetric(
        insertion: .opacity.animation(contentFadeIn),
        removal: .opacity.animation(contentFadeOut)
    )
}
