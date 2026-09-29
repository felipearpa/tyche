import SwiftUI
import Testing
import UIKit
@testable import UI

/// Primary-container surfaces (the manage-gamblers owner row, the join screen's pool pill, and the
/// selected pool-creation template) are #1B5E20 with white content in both appearances, with and
/// without Increase Contrast. The owner row's secondary line draws the content color at 70 %
/// opacity, so that blend must also reach 4.5:1.
struct PrimaryContainerColorTests {
    private static let traits: [UITraitCollection] = [UIUserInterfaceStyle.light, .dark].flatMap { style in
        [UIAccessibilityContrast.normal, .high].map { contrast in
            UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceStyle: style),
                UITraitCollection(accessibilityContrast: contrast),
            ])
        }
    }

    @Test(arguments: traits)
    func containerIsDarkGreenWithWhiteContent(traits: UITraitCollection) {
        #expect(hex(container(traits)) == 0x1B5E20)
        #expect(hex(content(traits)) == 0xFFFFFF)
    }

    @Test(arguments: traits)
    func contentIsLegibleOnTheContainer(traits: UITraitCollection) {
        let fill = container(traits)
        #expect(contrast(content(traits), fill) >= 4.5)
        #expect(contrast(blend(content(traits), over: fill, opacity: 0.7), fill) >= 4.5)
    }

    private func container(_ traits: UITraitCollection) -> UIColor {
        UIColor(Color(sharedResource: .primaryContainer)).resolvedColor(with: traits)
    }

    private func content(_ traits: UITraitCollection) -> UIColor {
        UIColor(Color(sharedResource: .onPrimaryContainter)).resolvedColor(with: traits)
    }
}

private func components(_ color: UIColor) -> (CGFloat, CGFloat, CGFloat) {
    var red: CGFloat = 0
    var green: CGFloat = 0
    var blue: CGFloat = 0
    var alpha: CGFloat = 0
    color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
    return (red, green, blue)
}

private func hex(_ color: UIColor) -> UInt32 {
    let (red, green, blue) = components(color)
    let channel = { (value: CGFloat) in UInt32((value * 255).rounded()) }
    return channel(red) << 16 | channel(green) << 8 | channel(blue)
}

private func blend(_ top: UIColor, over bottom: UIColor, opacity: CGFloat) -> UIColor {
    let (tr, tg, tb) = components(top)
    let (br, bg, bb) = components(bottom)
    return UIColor(
        red: tr * opacity + br * (1 - opacity),
        green: tg * opacity + bg * (1 - opacity),
        blue: tb * opacity + bb * (1 - opacity),
        alpha: 1
    )
}

private func contrast(_ first: UIColor, _ second: UIColor) -> CGFloat {
    let firstLuminance = relativeLuminance(first)
    let secondLuminance = relativeLuminance(second)
    return (max(firstLuminance, secondLuminance) + 0.05) / (min(firstLuminance, secondLuminance) + 0.05)
}

private func relativeLuminance(_ color: UIColor) -> CGFloat {
    let (red, green, blue) = components(color)
    func channel(_ value: CGFloat) -> CGFloat {
        value <= 0.03928 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}
