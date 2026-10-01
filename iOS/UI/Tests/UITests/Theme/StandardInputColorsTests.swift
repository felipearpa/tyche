import SwiftUI
import Testing
import UIKit
@testable import UI

/// The standard input replaces a glass capsule with semantic colors that are checked against every
/// screen background it is placed on (plain and grouped), in both appearances and at the elevated
/// level used by sheets. The fill is translucent, so it is composited over each background first.
struct StandardInputColorsTests {
    private static let traits: [UITraitCollection] = [UIUserInterfaceStyle.light, .dark].flatMap { style in
        [UIUserInterfaceLevel.base, .elevated].map { level in
            UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceStyle: style),
                UITraitCollection(userInterfaceLevel: level),
            ])
        }
    }

    private static let screenBackgrounds: [UIColor] = [.systemBackground, .systemGroupedBackground]

    @Test(arguments: traits)
    func enabledBoundaryIsDistinguishableFromTheScreen(traits: UITraitCollection) {
        let border = StandardInputColors.borderUIColor.resolvedColor(with: traits)
        for background in Self.screenBackgrounds {
            #expect(contrast(border, background.resolvedColor(with: traits)) >= 3)
        }
    }

    @Test(arguments: traits)
    func enteredTextIsLegibleWhetherEnabledOrDisabled(traits: UITraitCollection) {
        let text = UIColor.label.resolvedColor(with: traits)
        for background in Self.screenBackgrounds {
            let screen = background.resolvedColor(with: traits)
            // Enabled fields draw the fill over the screen; disabled fields show the screen.
            #expect(contrast(text, filled(over: screen, traits: traits)) >= 4.5)
            #expect(contrast(text, screen) >= 4.5)
        }
    }

    /// Visual separation only: the 3:1 boundary is what identifies the field. A fill equal to the
    /// screen color (ratio 1.0, as an opaque `secondarySystemBackground` was on grouped screens)
    /// fails; `tertiarySystemFill` measures between 1.14 and 1.33 across these backgrounds.
    @Test(arguments: traits)
    func fillIsDistinctFromPlainAndGroupedBackgrounds(traits: UITraitCollection) {
        for background in Self.screenBackgrounds {
            let screen = background.resolvedColor(with: traits)
            #expect(contrast(filled(over: screen, traits: traits), screen) >= 1.1)
        }
    }

    private func filled(over screen: UIColor, traits: UITraitCollection) -> UIColor {
        composite(StandardInputColors.fillUIColor.resolvedColor(with: traits), over: screen)
    }
}

private func contrast(_ first: UIColor, _ second: UIColor) -> CGFloat {
    let firstLuminance = relativeLuminance(first)
    let secondLuminance = relativeLuminance(second)
    return (max(firstLuminance, secondLuminance) + 0.05) / (min(firstLuminance, secondLuminance) + 0.05)
}

private func composite(_ top: UIColor, over bottom: UIColor) -> UIColor {
    var (tr, tg, tb, ta): (CGFloat, CGFloat, CGFloat, CGFloat) = (0, 0, 0, 0)
    var (br, bg, bb, ba): (CGFloat, CGFloat, CGFloat, CGFloat) = (0, 0, 0, 0)
    top.getRed(&tr, green: &tg, blue: &tb, alpha: &ta)
    bottom.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
    return UIColor(
        red: tr * ta + br * (1 - ta),
        green: tg * ta + bg * (1 - ta),
        blue: tb * ta + bb * (1 - ta),
        alpha: 1
    )
}

private func relativeLuminance(_ color: UIColor) -> CGFloat {
    var red: CGFloat = 0
    var green: CGFloat = 0
    var blue: CGFloat = 0
    var alpha: CGFloat = 0
    color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

    func channel(_ value: CGFloat) -> CGFloat {
        value <= 0.03928 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
    }

    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}
