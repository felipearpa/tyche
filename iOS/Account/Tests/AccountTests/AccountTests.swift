import XCTest
@testable import Account

final class AccountAvatarTests: XCTestCase {
    func testEmailIdentityKeepsExistingLocalPartBehavior() {
        XCTAssertEqual("felipe.arpa@example.com".emailAvatarIdentity, "felipe.arpa")
        XCTAssertEqual("@example.com".emailAvatarIdentity, "")
    }

    func testInitialUsesFirstUserPerceivedLetterOrNumber() {
        XCTAssertEqual("__élgoleador".avatarInitial, "É")
        XCTAssertEqual("⚽️ 88".avatarInitial, "8")
        XCTAssertNil("___".avatarInitial)
    }

    func testGeneratedAvatarColorsMeetEnhancedContrast() {
        for isDark in [false, true] {
            let colors = avatarRGBColors(
                for: "ElGoleador",
                isDark: isDark
            )
            XCTAssertGreaterThanOrEqual(
                contrastRatio(colors.background, colors.foreground),
                7
            )
        }
    }

    func testAvatarRequestUsesDeterministicURLAndRevalidation() {
        let request = avatarPhotoRequest(accountId: "account-1")

        XCTAssertEqual(
            request?.url?.absoluteString,
            "https://tyche-avatars.s3.us-east-2.amazonaws.com/avatars/account-1.jpg"
        )
        XCTAssertEqual(request?.cachePolicy, .reloadRevalidatingCacheData)
    }
}
