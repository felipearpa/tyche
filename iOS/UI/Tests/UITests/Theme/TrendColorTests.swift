import XCTest
import SwiftUI
import UIKit
@testable import UI

/// The trend tokens color the movement numbers as well as the arrows and the steady rule, so each
/// must reach the normal-text ratio, 4.5:1, on every surface `TrendIndicator` sits on: the canvas,
/// the group badge's fill, and the signed-in gambler's highlighted leaderboard row.
final class TrendColorTests: XCTestCase {
    func testTrendTokensMeetNormalTextContrastOnEverySurfaceInBothAppearances() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let traits = UITraitCollection(userInterfaceStyle: style)
            let canvas = UIColor.systemBackground.resolvedColor(with: traits)
            let surfaces = [
                ("canvas", canvas),
                ("badge", composite(UIColor.secondarySystemFill.resolvedColor(with: traits), over: canvas)),
                ("current-user row", UIColor(Color(.currentUserContainer)).resolvedColor(with: traits)),
            ]
            let tokens = [("positive", Color(.positive)), ("negative", Color(.negative)), ("steady", Color(.neutral))]

            for (tokenName, token) in tokens {
                for (surfaceName, surface) in surfaces {
                    XCTAssertGreaterThanOrEqual(
                        contrast(UIColor(token).resolvedColor(with: traits), surface),
                        4.5,
                        "\(tokenName) on \(surfaceName), \(style == .light ? "light" : "dark")"
                    )
                }
            }
        }
    }
}

private func composite(_ foreground: UIColor, over background: UIColor) -> UIColor {
    var (fr, fg, fb, fa): (CGFloat, CGFloat, CGFloat, CGFloat) = (0, 0, 0, 0)
    var (br, bg, bb, ba): (CGFloat, CGFloat, CGFloat, CGFloat) = (0, 0, 0, 0)
    foreground.getRed(&fr, green: &fg, blue: &fb, alpha: &fa)
    background.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
    return UIColor(
        red: fr * fa + br * (1 - fa),
        green: fg * fa + bg * (1 - fa),
        blue: fb * fa + bb * (1 - fa),
        alpha: 1
    )
}

private func contrast(_ first: UIColor, _ second: UIColor) -> CGFloat {
    let firstLuminance = relativeLuminance(first)
    let secondLuminance = relativeLuminance(second)
    let brighter = max(firstLuminance, secondLuminance)
    let darker = min(firstLuminance, secondLuminance)
    return (brighter + 0.05) / (darker + 0.05)
}

private func relativeLuminance(_ color: UIColor) -> CGFloat {
    var red: CGFloat = 0
    var green: CGFloat = 0
    var blue: CGFloat = 0
    var alpha: CGFloat = 0
    color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

    func channel(_ value: CGFloat) -> CGFloat {
        value <= 0.03928
            ? value / 12.92
            : pow((value + 0.055) / 1.055, 2.4)
    }

    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}
