import XCTest

/// Walks both drawers against the signed-in simulator session: the drawer's actions exist only
/// while it is open, the pushed screen cannot be hit while it is open, a tap on the pushed screen
/// closes it without reaching the screen's controls, and a destination opens once and returns
/// with the drawer closed. Sign out is only checked for reachability; it is never tapped, and no
/// drag starts on it.
///
/// The gesture passes use real synthesized drags, so they exercise the arbitration between the
/// drawer's drag and the rows, scroll views, tab bar, and back gestures it overlaps.
///
/// The pushed screen is checked with `isHittable`, not `exists`: XCUITest's automation tree
/// still lists native navigation descendants hidden from VoiceOver by their hosting boundary,
/// while it drops the drawer's own hidden SwiftUI content.
final class DrawerPassUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false

        // Skipped by default: the pass needs a signed-in session and real pools on the target
        // simulator. Run on demand with `xcodebuild test ... TEST_RUNNER_DRAWER_UI_PASS=1`.
        try XCTSkipUnless(
            ProcessInfo.processInfo.environment["DRAWER_UI_PASS"] == "1",
            "set DRAWER_UI_PASS=1 to run the drawer pass"
        )
    }

    @MainActor
    func testDrawerNavigationLayout() throws {
        let app = XCUIApplication()
        XCUIDevice.shared.orientation = .portrait
        defer { XCUIDevice.shared.orientation = .portrait }
        app.launch()
        let opener = app.buttons["Open menu"]
        XCTAssertTrue(opener.waitForExistence(timeout: 20), app.debugDescription)

        for host in ["list", "home"] {
            if host == "home" {
                let row = firstPoolRow(in: app)
                XCTAssertTrue(row.waitForExistence(timeout: 20))
                row.tap()
                XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 20))
            }
            for orientation in [UIDeviceOrientation.portrait, .landscapeLeft, .landscapeRight] {
                XCUIDevice.shared.orientation = orientation
                XCTAssertTrue(waitUntilHittable(opener))
                // Let the system's rotation and navigation-bar size transition finish first.
                Thread.sleep(forTimeInterval: 1)
                let bar = app.navigationBars.firstMatch
                let title = bar.staticTexts.firstMatch
                XCTAssertTrue(title.exists)
                let closedBar = bar.frame
                let closedTitle = title.frame
                let closedOpener = opener.frame

                for cycle in 0..<2 {
                    opener.tap()
                    XCTAssertTrue(waitUntilHittable(app.buttons["Profile"]))
                    let openBar = bar.frame
                    XCTAssertGreaterThan(openBar.minX - closedBar.minX, 250)
                    XCTAssertEqual(openBar.width, closedBar.width, accuracy: 1)
                    XCTAssertEqual(
                        title.frame.minX - openBar.minX,
                        closedTitle.minX - closedBar.minX,
                        accuracy: 1,
                        "\(host): title shifted within the pushed screen"
                    )
                    XCTAssertEqual(
                        opener.frame.minX - openBar.minX,
                        closedOpener.minX - closedBar.minX,
                        accuracy: 1,
                        "\(host): avatar shifted within the pushed screen"
                    )
                    XCTAssertFalse(opener.isHittable, "\(host): modal drawer leaves the background interactive")
                    if cycle == 0 {
                        attach(app, name: "stable-navigation-\(host)-\(orientation.rawValue)")
                    }
                    app.buttons["Close menu"].tap()
                    XCTAssertTrue(waitUntilHittable(opener))
                    XCTAssertEqual(title.frame.minX, closedTitle.minX, accuracy: 1)
                    XCTAssertEqual(opener.frame.minX, closedOpener.minX, accuracy: 1)
                    XCTAssertEqual(bar.frame.width, closedBar.width, accuracy: 1)
                }
            }
            XCUIDevice.shared.orientation = .portrait
        }
    }

    @MainActor
    func testPoolListDrawer() throws {
        let app = XCUIApplication()
        app.launch()

        let opener = app.buttons["Open menu"]
        XCTAssertTrue(opener.waitForExistence(timeout: 20), "pool list did not appear")
        let poolRow = firstPoolRow(in: app)
        XCTAssertTrue(poolRow.waitForExistence(timeout: 20), "no pool rows to push aside")
        XCTAssertFalse(app.buttons["Profile"].exists, "closed drawer exposes its actions")
        XCTAssertFalse(app.buttons["Close menu"].exists, "closed drawer exposes its dismissal")

        opener.tap()
        let profile = app.buttons["Profile"]
        XCTAssertTrue(profile.waitForExistence(timeout: 5), "drawer did not open")
        attach(app, name: "pool-list-drawer-open")
        XCTAssertTrue(app.buttons["Sign out"].isHittable, "Sign out is not reachable")
        XCTAssertTrue(app.buttons["Close menu"].exists, "open drawer has no dismissal action")
        XCTAssertFalse(opener.isHittable, "pushed screen stays interactive while the drawer is open")
        XCTAssertFalse(poolRow.isHittable, "pushed screen stays interactive while the drawer is open")

        // The visible strip sits over a pool row; the tap must only close the drawer.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.96, dy: 0.28)).tap()
        XCTAssertTrue(waitUntilHittable(opener), "tapping the pushed screen did not close")
        XCTAssertFalse(profile.exists)
        XCTAssertTrue(poolRow.isHittable, "the tap reached the pool row underneath")

        opener.tap()
        XCTAssertTrue(profile.waitForExistence(timeout: 5))
        profile.tap()
        XCTAssertTrue(app.buttons["Change Photo"].waitForExistence(timeout: 10), "Profile did not open")

        // One Back returns to the pool list only if Profile was pushed exactly once.
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(waitUntilHittable(poolRow), "Profile was pushed more than once")
        XCTAssertFalse(profile.exists, "drawer reopened after returning from Profile")
    }

    @MainActor
    func testPoolHomeDrawer() throws {
        let app = XCUIApplication()
        app.launch()

        let poolRows = app.buttons.matching(poolRowPredicate)
        XCTAssertTrue(poolRows.firstMatch.waitForExistence(timeout: 20), "no pools to open")
        let poolCount = min(poolRows.count, 3)
        var sawOwnedPool = false

        for index in 0..<poolCount {
            poolRows.element(boundBy: index).tap()
            XCTAssertTrue(app.tabBars.firstMatch.waitForExistence(timeout: 20), "pool home did not appear")
            let opener = app.buttons["Open menu"]
            XCTAssertTrue(opener.waitForExistence(timeout: 10))

            opener.tap()
            let invite = app.buttons["Invite"]
            XCTAssertTrue(invite.waitForExistence(timeout: 5), "pool drawer did not open")
            let summary = app.descendants(matching: .any)
                .matching(NSPredicate(format: "label BEGINSWITH %@", "Playing now"))
                .firstMatch
            XCTAssertTrue(summary.waitForExistence(timeout: 20), "pool summary did not load")
            XCTAssertTrue(app.buttons["Sign out"].isHittable, "Sign out is not reachable")

            let isOwner = app.buttons["Delete pool"].exists
            attach(app, name: "pool-home-drawer-\(index)-\(isOwner ? "owner" : "member")")
            // The Gamblers row announces its member count after the label.
            let gamblers = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Gamblers"))
            XCTAssertEqual(gamblers.firstMatch.exists, isOwner, "owner actions shown inconsistently")

            if isOwner {
                sawOwnedPool = true
                app.buttons["Delete pool"].tap()
                let confirmation = app.alerts["Delete pool?"]
                XCTAssertTrue(confirmation.waitForExistence(timeout: 5), "deletion was not confirmed first")
                confirmation.buttons["Cancel"].tap()
                XCTAssertFalse(confirmation.exists)
                XCTAssertTrue(app.buttons["Delete pool"].isHittable, "cancel left deletion pending")
            }

            // Invite closes the drawer and opens the share sheet, which is dismissed unsent.
            invite.tap()
            let shareSheet = app.otherElements["ActivityListView"]
            XCTAssertTrue(shareSheet.waitForExistence(timeout: 10), "share sheet did not open")
            attach(app, name: "pool-home-invite-\(index)")
            dismissShareSheet(in: app, shareSheet: shareSheet)
            XCTAssertTrue(waitUntilHittable(opener), "drawer did not close for Invite")
            XCTAssertFalse(invite.exists, "drawer reopened after the share sheet")

            app.navigationBars.buttons
                .matching(NSPredicate(format: "label != %@", "Open menu"))
                .firstMatch
                .tap()
            XCTAssertTrue(poolRows.firstMatch.waitForExistence(timeout: 20), "did not return to pools")
        }

        if !sawOwnedPool {
            XCTContext.runActivity(named: "No owned pool among the first \(poolCount)") { _ in }
        }
    }

    /// Swipes the pool list's drawer open from a pool row and closed from Profile without
    /// activating either, leaves leading and vertical drags to the list, still opens by swipe
    /// after those declined drags, and keeps Profile's back-swipe for navigation.
    @MainActor
    func testPoolListDrawerGestures() throws {
        let app = XCUIApplication()
        app.launch()

        let opener = app.buttons["Open menu"]
        XCTAssertTrue(opener.waitForExistence(timeout: 20), "pool list did not appear")
        let poolRow = firstPoolRow(in: app)
        XCTAssertTrue(poolRow.waitForExistence(timeout: 20), "no pool rows to swipe from")
        let profile = app.buttons["Profile"]
        let profileScreen = app.buttons["Change Photo"]

        drag(from: poolRow, at: 0.15, dx: 260)
        XCTAssertTrue(waitUntilHittable(profile), "a swipe from a pool row did not open the drawer")
        XCTAssertFalse(app.tabBars.firstMatch.exists, "the swipe opened the pool it started on")
        attach(app, name: "pool-list-swiped-open")

        drag(from: profile, at: 0.85, dx: -260)
        XCTAssertTrue(waitUntilHittable(opener), "a drag from Profile did not close the drawer")
        XCTAssertFalse(profile.exists, "the drawer stayed open")
        XCTAssertFalse(profileScreen.exists, "the drag opened Profile")

        drag(from: poolRow, at: 0.85, dx: -260)
        XCTAssertFalse(profile.waitForExistence(timeout: 2), "a leading swipe opened the drawer")
        XCTAssertFalse(app.tabBars.firstMatch.exists, "a leading swipe opened the pool")

        // A declined drag hands its touch back without leaving the drawer's drag detached: after
        // quick leading and vertical flicks on a row, a swipe from that row still opens the
        // drawer, and a drag on the pushed screen closes it.
        poolRow.swipeLeft(velocity: .fast)
        poolRow.swipeUp(velocity: .fast)
        XCTAssertFalse(profile.waitForExistence(timeout: 1), "a flick opened the drawer")
        XCTAssertFalse(app.tabBars.firstMatch.exists, "a flick opened the pool")
        drag(from: poolRow, at: 0.15, dx: 260)
        XCTAssertTrue(waitUntilHittable(profile), "a swipe after declined drags did not open the drawer")
        drag(from: app.coordinate(withNormalizedOffset: CGVector(dx: 0.96, dy: 0.28)), dx: -200)
        XCTAssertTrue(waitUntilHittable(opener), "a drag on the pushed screen did not close the drawer")
        XCTAssertFalse(app.tabBars.firstMatch.exists, "the drag on the pushed screen opened a pool")

        // Profile keeps its back-swipes: the edge, and on iOS 26 and later the content area.
        for (name, startX) in backSwipes() {
            opener.tap()
            XCTAssertTrue(waitUntilHittable(profile), "drawer did not open")
            profile.tap()
            XCTAssertTrue(profileScreen.waitForExistence(timeout: 10), "Profile did not open")
            let start = app.coordinate(withNormalizedOffset: CGVector(dx: startX, dy: 0.55))
            drag(from: start, dx: 300)
            XCTAssertTrue(waitUntilHittable(poolRow), "the \(name) back-swipe on Profile did not return")
            XCTAssertFalse(profile.exists, "the \(name) back-swipe opened the drawer")
        }

        // A handful of pools fits on screen and cannot scroll, so the vertical drag runs with the
        // largest accessibility text, where the list overflows.
        app.terminate()
        app.launchArguments = ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        app.launch()
        XCTAssertTrue(poolRow.waitForExistence(timeout: 20), "pool list did not appear with the largest text")
        // The list scrolls only if it recognizes the drag before the drawer's 20-point drag
        // does. A main-thread stall while the relaunch is still loading can deliver the first 20
        // points in one event, and the list then stays put for that touch, so the drag waits for
        // the relaunch to settle and moves slowly.
        Thread.sleep(forTimeInterval: 3)
        let rowTop = poolRow.frame.minY
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75))
        start.press(
            forDuration: 0.05,
            thenDragTo: start.withOffset(CGVector(dx: 0, dy: -300)),
            withVelocity: .slow,
            thenHoldForDuration: 0.05
        )
        XCTAssertFalse(profile.waitForExistence(timeout: 2), "a vertical drag opened the drawer")
        XCTAssertFalse(app.tabBars.firstMatch.exists, "a vertical drag opened a pool")
        XCTAssertLessThan(
            poolRow.frame.minY,
            rowTop - 50,
            "the pool list did not scroll (or its rows fit on screen even with the largest text)"
        )
    }

    /// Swipes the pool home's drawer open from another gambler's row and closed from Invite
    /// without activating either, keeps the tab bar's taps and drags and the leaderboard's
    /// scrolling, and keeps a gambler's bets reachable by tap and left by back-swipe.
    @MainActor
    func testPoolHomeDrawerGestures() throws {
        let app = XCUIApplication()
        app.launch()

        let poolRows = app.buttons.matching(poolRowPredicate)
        XCTAssertTrue(poolRows.firstMatch.waitForExistence(timeout: 20), "no pools to open")
        poolRows.firstMatch.tap()
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 20), "pool home did not appear")
        let opener = app.buttons["Open menu"]
        let invite = app.buttons["Invite"]
        let otherGambler = app.buttons.matching(
            NSPredicate(format: "label BEGINSWITH %@ AND NOT (label CONTAINS %@)", "Rank ", ", You,")
        ).firstMatch
        XCTAssertTrue(
            otherGambler.waitForExistence(timeout: 20),
            "the first pool has no other gamblers to drag from"
        )

        drag(from: otherGambler, at: 0.15, dx: 260)
        XCTAssertTrue(waitUntilHittable(invite), "a swipe from a gambler row did not open the drawer")
        attach(app, name: "pool-home-swiped-open")

        drag(from: invite, at: 0.85, dx: -260)
        XCTAssertTrue(waitUntilHittable(opener), "a drag from Invite did not close the drawer")
        XCTAssertFalse(app.otherElements["ActivityListView"].waitForExistence(timeout: 2), "the drag invited")
        XCTAssertTrue(tabBar.isHittable, "the swipe opened the gambler it started on")

        // Tab bar: a tap switches tabs; on iOS 26 and later a finger sliding along the bar does too.
        let tabs = tabBar.buttons
        tabs.element(boundBy: 1).tap()
        XCTAssertTrue(tabs.element(boundBy: 1).isSelected, "tapping a tab did not select it")
        drag(from: tabs.element(boundBy: 1).coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)),
             to: tabs.element(boundBy: 2).coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)),
             holdingFirst: 0.2)
        XCTAssertFalse(invite.waitForExistence(timeout: 2), "sliding along the tab bar opened the drawer")
        if #available(iOS 26.0, *) {
            XCTAssertTrue(tabs.element(boundBy: 2).isSelected, "sliding along the tab bar did not switch tabs")
        }
        attach(app, name: "pool-home-tab-bar-slide")
        tabs.element(boundBy: 0).tap()
        XCTAssertTrue(otherGambler.waitForExistence(timeout: 20))

        let firstRow = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label BEGINSWITH %@", "Rank 1,"))
            .firstMatch
        XCTAssertTrue(firstRow.waitForExistence(timeout: 10))
        let firstRowTop = firstRow.frame.minY
        drag(from: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.7)), dx: 0, dy: -250)
        XCTAssertFalse(invite.waitForExistence(timeout: 2), "a vertical drag opened the drawer")
        XCTAssertTrue(tabBar.isHittable, "a vertical drag opened a gambler")
        XCTAssertLessThan(firstRow.frame.minY, firstRowTop - 50, "the leaderboard did not scroll")
        drag(from: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4)), dx: 0, dy: 400)

        // The drawer declined the tab bar's slide and the vertical drags; a swipe still opens it
        // afterwards, and a tap on the pushed screen closes it without opening a gambler.
        drag(from: otherGambler, at: 0.15, dx: 260)
        XCTAssertTrue(waitUntilHittable(invite), "a swipe after declined drags did not open the drawer")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.96, dy: 0.5)).tap()
        XCTAssertTrue(waitUntilHittable(opener), "tapping the pushed screen did not close the drawer")
        XCTAssertTrue(tabBar.isHittable, "the swipe or the tap opened a gambler")

        // A tap still opens the gambler's bets once, and each back-swipe returns without opening
        // the drawer.
        for (name, startX) in backSwipes() {
            otherGambler.tap()
            XCTAssertTrue(tabBar.waitForNonExistence(timeout: 10), "tapping a gambler did not open their bets")
            drag(from: app.coordinate(withNormalizedOffset: CGVector(dx: startX, dy: 0.55)), dx: 300)
            XCTAssertTrue(
                waitUntilHittable(tabBar),
                "the \(name) back-swipe did not return, or the gambler opened twice"
            )
            XCTAssertFalse(invite.exists, "the \(name) back-swipe opened the drawer")
        }
    }

    /// The back-swipes a destination supports, with where each starts across the screen's
    /// width: the leading edge everywhere, and the content area from iOS 26.
    private func backSwipes() -> [(name: String, startX: CGFloat)] {
        if #available(iOS 26.0, *) {
            return [("content-area", 0.3), ("edge", 0.005)]
        }
        return [("edge", 0.005)]
    }

    private var poolRowPredicate: NSPredicate {
        // "My pools" rows announce "<name>, Rank …, <n> members"; leaderboard rows lead with the
        // rank and carry no member count.
        NSPredicate(format: "label CONTAINS %@ AND label CONTAINS %@", ", Rank", " member")
    }

    @MainActor
    private func firstPoolRow(in app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(poolRowPredicate).firstMatch
    }

    /// Presses briefly and drags horizontally from a point across `element`'s width.
    @MainActor
    private func drag(from element: XCUIElement, at normalizedX: CGFloat, dx: CGFloat) {
        drag(from: element.coordinate(withNormalizedOffset: CGVector(dx: normalizedX, dy: 0.5)), dx: dx)
    }

    @MainActor
    private func drag(from start: XCUICoordinate, dx: CGFloat, dy: CGFloat = 0) {
        drag(from: start, to: start.withOffset(CGVector(dx: dx, dy: dy)))
    }

    @MainActor
    private func drag(from start: XCUICoordinate, to end: XCUICoordinate, holdingFirst hold: TimeInterval = 0.05) {
        start.press(forDuration: hold, thenDragTo: end, withVelocity: .default, thenHoldForDuration: 0.05)
    }

    @MainActor
    private func waitUntilHittable(_ element: XCUIElement, timeout: TimeInterval = 10) -> Bool {
        let hittable = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "isHittable == true"),
            object: element
        )
        return XCTWaiter().wait(for: [hittable], timeout: timeout) == .completed
    }

    @MainActor
    private func dismissShareSheet(in app: XCUIApplication, shareSheet: XCUIElement) {
        let close = shareSheet.buttons["Close"]
        if close.exists {
            close.tap()
        } else {
            shareSheet.swipeDown(velocity: .fast)
        }
        XCTAssertTrue(shareSheet.waitForNonExistence(timeout: 10), "share sheet did not dismiss")
        waitForMeasurableWindow(in: app)
    }

    /// Right after the share sheet goes away, XCUITest can report the app's window with an
    /// infinite frame, and a hittability check then fails outright instead of waiting. Waits
    /// until the window has a real frame again.
    @MainActor
    private func waitForMeasurableWindow(in app: XCUIApplication, timeout: TimeInterval = 10) {
        let window = app.windows.firstMatch
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let frame = window.frame
            if frame.minX.isFinite, frame.minY.isFinite, frame.width > 0, frame.height > 0 {
                return
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.25))
        }
        XCTFail("the app's window never reported a frame after the share sheet closed")
    }

    @MainActor
    private func attach(_ app: XCUIApplication, name: String) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)

        let hierarchy = XCTAttachment(string: app.debugDescription)
        hierarchy.name = "\(name)-accessibility"
        hierarchy.lifetime = .keepAlways
        add(hierarchy)
    }
}
