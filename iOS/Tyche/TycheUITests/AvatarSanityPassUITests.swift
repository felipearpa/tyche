import UIKit
import XCTest

/// Walks the avatar flow end to end against the signed-in simulator session:
/// drawer → Profile → source chooser (camera hidden on simulator) → library pick →
/// crop (pinch/pan + confirm) → upload → Profile refresh → remote re-fetch on re-entry.
final class AvatarSanityPassUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false

        // Skipped by default: this pass uploads a real avatar to the live bucket and needs a
        // signed-in session on the target simulator. Run on demand with
        // `xcodebuild test ... TEST_RUNNER_AVATAR_SANITY_PASS=1` (or the env var in a test plan).
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["AVATAR_SANITY_PASS"] == "1",
            "set AVATAR_SANITY_PASS=1 to run the avatar sanity pass"
        )
    }

    @MainActor
    func testAvatarUploadFlow() throws {
        let app = XCUIApplication()
        app.launch()

        let navigationBar = app.navigationBars.firstMatch
        XCTAssertTrue(navigationBar.waitForExistence(timeout: 20), "pool screen did not appear")
        navigationBar.buttons.element(boundBy: 0).tap()

        let profileRow = app.buttons["Profile"]
        XCTAssertTrue(profileRow.waitForExistence(timeout: 10), "Profile drawer row missing")
        profileRow.tap()

        let changePhoto = app.buttons["Change Photo"]
        XCTAssertTrue(changePhoto.waitForExistence(timeout: 10), "Profile screen did not appear")
        attach(app, name: "1-profile")
        changePhoto.tap()

        let chooseFromLibrary = app.buttons["Choose from Library"]
        XCTAssertTrue(chooseFromLibrary.waitForExistence(timeout: 10), "source chooser missing")
        XCTAssertEqual(
            app.buttons["Take Photo"].exists,
            UIImagePickerController.isSourceTypeAvailable(.camera),
            "camera row must match UIImagePickerController availability"
        )
        attach(app, name: "2-source-chooser")
        chooseFromLibrary.tap()

        // The picker's privacy banner exposes an icon through the same image queries as the
        // photo grid, so pick by geometry: the first photo-sized image below the banner area.
        XCTAssertTrue(app.images.firstMatch.waitForExistence(timeout: 15), "picker showed no images")
        sleep(2)

        let gridPhoto = app.images.allElementsBoundByIndex.first { image in
            image.frame.minY > app.frame.height * 0.35 && image.frame.width > 80
        }

        if let gridPhoto {
            gridPhoto.tap()
        } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.17, dy: 0.47)).tap()
        }

        let usePhoto = app.buttons["Use Photo"]
        XCTAssertTrue(usePhoto.waitForExistence(timeout: 20), "crop screen did not appear")
        attach(app, name: "3-crop")

        let window = app.windows.firstMatch
        window.pinch(withScale: 1.5, velocity: 1)
        window.swipeLeft()
        attach(app, name: "4-crop-after-gestures")

        XCTAssertTrue(usePhoto.isHittable, "Use Photo must stay tappable after gestures")
        usePhoto.tap()

        XCTAssertTrue(changePhoto.waitForExistence(timeout: 15), "did not return to Profile")
        XCTAssertFalse(
            app.alerts.firstMatch.waitForExistence(timeout: 8),
            "upload error alert appeared"
        )
        attach(app, name: "5-profile-after-upload")

        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(navigationBar.waitForExistence(timeout: 10))
        navigationBar.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(profileRow.waitForExistence(timeout: 10))
        profileRow.tap()

        XCTAssertTrue(changePhoto.waitForExistence(timeout: 10), "Profile re-entry failed")
        sleep(3)
        attach(app, name: "6-profile-reentry-remote-fetch")
    }

    /// Visual pass over the navigation chrome: the toolbar avatar, drawer header,
    /// and Profile screen must show the account's photo instead of the letter avatar.
    @MainActor
    func testChromeShowsAvatarPhoto() throws {
        let app = XCUIApplication()
        app.launch()

        let navigationBar = app.navigationBars.firstMatch
        XCTAssertTrue(navigationBar.waitForExistence(timeout: 20), "pool screen did not appear")
        sleep(3)
        attach(app, name: "chrome-1-toolbar")

        navigationBar.buttons.element(boundBy: 0).tap()
        let profileRow = app.buttons["Profile"]
        XCTAssertTrue(profileRow.waitForExistence(timeout: 10), "drawer did not open")
        sleep(2)
        attach(app, name: "chrome-2-drawer")

        profileRow.tap()
        XCTAssertTrue(app.buttons["Change Photo"].waitForExistence(timeout: 10))
        sleep(3)
        attach(app, name: "chrome-3-profile")
    }

    @MainActor
    private func attach(_ app: XCUIApplication, name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
