import XCTest
import SwiftUI
import UIKit
@testable import UI

final class CurrentUserColorTests: XCTestCase {
    func testCurrentUserPairsMeetNormalTextContrastInBothAppearances() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let traits = UITraitCollection(userInterfaceStyle: style)

            XCTAssertGreaterThanOrEqual(
                contrast(
                    UIColor(Color(.onCurrentUser)).resolvedColor(with: traits),
                    UIColor(Color(.currentUser)).resolvedColor(with: traits)
                ),
                4.5
            )
            XCTAssertGreaterThanOrEqual(
                contrast(
                    UIColor(Color(.onCurrentUserContainer)).resolvedColor(with: traits),
                    UIColor(Color(.currentUserContainer)).resolvedColor(with: traits)
                ),
                4.5
            )
        }
    }
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
