import Testing
import UIKit

/// The app accent is #2E7D32 in light appearance and #4CAF50 in dark appearance, and keeps those
/// values with Increase Contrast, so the system doesn't lighten the fill under an accent-filled
/// label. The label itself is measured against these fills in the UI package's
/// `AccentFilledLabelTests`.
struct AccentColorTests {
    struct Case: CustomTestStringConvertible, Sendable {
        let style: UIUserInterfaceStyle
        let contrast: UIAccessibilityContrast
        let expectedHex: UInt32

        var traits: UITraitCollection {
            UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceStyle: style),
                UITraitCollection(accessibilityContrast: contrast),
            ])
        }

        var testDescription: String {
            "\(style == .dark ? "dark" : "light"), \(contrast == .high ? "high contrast" : "normal contrast")"
        }
    }

    private static let cases: [Case] = [
        Case(style: .light, contrast: .normal, expectedHex: 0x2E7D32),
        Case(style: .light, contrast: .high, expectedHex: 0x2E7D32),
        Case(style: .dark, contrast: .normal, expectedHex: 0x4CAF50),
        Case(style: .dark, contrast: .high, expectedHex: 0x4CAF50),
    ]

    @Test(arguments: cases)
    func accentHasTheBrandValue(_ testCase: Case) throws {
        let accent = try #require(UIColor(named: "AccentColor")).resolvedColor(with: testCase.traits)
        #expect(hex(accent) == testCase.expectedHex)
    }
}

private func hex(_ color: UIColor) -> UInt32 {
    var red: CGFloat = 0
    var green: CGFloat = 0
    var blue: CGFloat = 0
    var alpha: CGFloat = 0
    color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
    let channel = { (value: CGFloat) in UInt32((value * 255).rounded()) }
    return channel(red) << 16 | channel(green) << 8 | channel(blue)
}
