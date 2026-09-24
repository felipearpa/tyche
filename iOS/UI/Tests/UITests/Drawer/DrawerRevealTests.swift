import CoreGraphics
import SwiftUI
import Testing
@testable import UI

/// The reveal model behind `drawer(isShowing:content:)`: requests settle at an endpoint, a drag
/// takes over from the progress on screen, and releases, cancellations, and interruptions land
/// on the endpoint the specification requires. SwiftUI animates between the model values these
/// tests pin; the takeover values are what keep an interrupted transition from jumping. Which
/// drags the drawer claims decides whether the control under the finger is cancelled or keeps
/// the touch; `DrawerPassUITests` exercises that arbitration with real drags.
struct DrawerRevealTests {
    private let width: CGFloat = 340

    @Test
    func requestsSettleAtTheRequestedEndpoint() {
        var reveal = DrawerReveal(isOpen: false)

        reveal.settle(isOpen: true)
        #expect(reveal.progress == 1)
        #expect(reveal.isResting(isOpen: true))

        reveal.settle(isOpen: false)
        #expect(reveal.progress == 0)
        #expect(reveal.isResting(isOpen: false))
    }

    @Test
    func reversingAnOpeningTargetsTheClosedEndpointWithoutPassingThroughOpen() {
        var reveal = DrawerReveal(isOpen: false)
        reveal.settle(isOpen: true)

        // A close request arrives while the opening is still on screen.
        reveal.settle(isOpen: false)

        #expect(reveal.progress == 0)
        #expect(DrawerPhase(presentedProgress: 0.4, reveal: reveal) == .closing)
    }

    @Test
    func dragTakesOverFromThePresentedProgressWithoutJumping() {
        var reveal = DrawerReveal(isOpen: false)
        reveal.settle(isOpen: true)

        // The opening is 40% on screen when the finger, already 12 points along, is recognized.
        reveal.updateDrag(translation: CGSize(width: -12, height: 1), presentedProgress: 0.4, width: width)
        #expect(reveal.progress == 0.4)
        #expect(reveal.isTracking)

        // From there the reveal follows the finger one to one.
        reveal.updateDrag(translation: CGSize(width: -46, height: 2), presentedProgress: 0.4, width: width)
        #expect(abs(reveal.progress - 0.3) < 0.0001)
    }

    @Test
    func releaseSettlesByDisplacement() throws {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: -10, height: 0), presentedProgress: 1, width: width)
        reveal.updateDrag(translation: CGSize(width: -200, height: 0), presentedProgress: 1, width: width)

        let release = reveal.endDrag(predictedTranslation: -200, width: width)
        let isOpen = try #require(release as Bool?)

        #expect(isOpen == false)
        #expect(reveal.drag == nil)
    }

    @Test
    func releaseHonorsVelocityOverAShortDisplacement() throws {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: -10, height: 0), presentedProgress: 1, width: width)
        reveal.updateDrag(translation: CGSize(width: -60, height: 0), presentedProgress: 1, width: width)

        // Only 60 points moved, but the flick projects well past the midpoint.
        let release = reveal.endDrag(predictedTranslation: -260, width: width)
        let isOpen = try #require(release as Bool?)

        #expect(isOpen == false)
    }

    @Test
    func slowShortReleaseReturnsToTheOpenEndpoint() throws {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: -10, height: 0), presentedProgress: 1, width: width)
        reveal.updateDrag(translation: CGSize(width: -60, height: 0), presentedProgress: 1, width: width)

        let release = reveal.endDrag(predictedTranslation: -62, width: width)
        let isOpen = try #require(release as Bool?)

        #expect(isOpen == true)
    }

    @Test
    func cancelledDragReturnsToTheSettledEndpoint() {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: -10, height: 0), presentedProgress: 1, width: width)
        reveal.updateDrag(translation: CGSize(width: -250, height: 0), presentedProgress: 1, width: width)

        let endpoint = reveal.cancelDrag()
        #expect(endpoint == true)
        #expect(reveal.drag == nil)
    }

    @Test
    func cancellingAGrabbedOpeningReturnsClosedWhereTheDrawerLastRested() {
        // Closed and settled; the menu button starts an opening.
        var reveal = DrawerReveal(isOpen: false)
        reveal.settle(isOpen: true)

        // The finger catches the opening at 30% and pulls it back to about 10%.
        reveal.updateDrag(translation: CGSize(width: -20, height: 0), presentedProgress: 0.3, width: width)
        reveal.updateDrag(translation: CGSize(width: -88, height: 0), presentedProgress: 0.3, width: width)

        // The system cancels the touch: the drawer returns closed, not to the requested open.
        #expect(reveal.cancelDrag() == false)
    }

    @Test
    func cancellingAGrabbedClosingReturnsOpenWhereTheDrawerLastRested() {
        var reveal = DrawerReveal(isOpen: false)
        reveal.settle(isOpen: true)
        reveal.noteSettled(isOpen: true)

        // A tap on the pushed screen starts the closing; the finger catches it at 70%.
        reveal.settle(isOpen: false)
        reveal.updateDrag(translation: CGSize(width: 20, height: 0), presentedProgress: 0.7, width: width)

        #expect(reveal.cancelDrag() == true)
    }

    @Test
    func finishingAfterAReleaseIsNotACancellation() {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: -10, height: 0), presentedProgress: 1, width: width)
        _ = reveal.endDrag(predictedTranslation: -10, width: width)

        #expect(reveal.cancelDrag() == nil)
    }

    @Test
    func verticalDragIsDeclinedAndIgnoredUntilItEnds() {
        var reveal = DrawerReveal(isOpen: true)

        let claimed = reveal.updateDrag(translation: CGSize(width: -3, height: 14), presentedProgress: 1, width: width)
        #expect(claimed == false)
        reveal.updateDrag(translation: CGSize(width: -200, height: 20), presentedProgress: 1, width: width)

        #expect(reveal.progress == 1)
        #expect(reveal.isTracking == false)
        let release = reveal.endDrag(predictedTranslation: -200, width: width)
        #expect(release == nil)
        #expect(reveal.drag == nil)
    }

    @Test
    func closedDrawerOpensFromADragTowardTheTrailingEdge() {
        var reveal = DrawerReveal(isOpen: false)

        // Recognized 20 points along, over whatever row the finger started on.
        let claimed = reveal.updateDrag(translation: CGSize(width: 20, height: 2), presentedProgress: 0, width: width)
        #expect(claimed)
        #expect(reveal.isTracking)
        #expect(reveal.progress == 0)

        reveal.updateDrag(translation: CGSize(width: 190, height: 4), presentedProgress: 0, width: width)
        #expect(abs(reveal.progress - 0.5) < 0.0001)
    }

    @Test
    func closedDrawerDeclinesADragTowardTheLeadingEdge() {
        var reveal = DrawerReveal(isOpen: false)

        let claimed = reveal.updateDrag(translation: CGSize(width: -20, height: 2), presentedProgress: 0, width: width)
        #expect(claimed == false)
        #expect(reveal.isTracking == false)

        // Turning back toward the trailing edge within the same drag does not open it.
        reveal.updateDrag(translation: CGSize(width: 150, height: 2), presentedProgress: 0, width: width)
        #expect(reveal.progress == 0)
        #expect(reveal.endDrag(predictedTranslation: 150, width: width) == nil)
    }

    @Test
    func closedDrawerLeavesTheHostsReservedBarAlone() {
        var reveal = DrawerReveal(isOpen: false)

        let claimed = reveal.updateDrag(
            translation: CGSize(width: 20, height: 0),
            presentedProgress: 0,
            width: width,
            startsInExcludedRegion: true
        )

        #expect(claimed == false)
        #expect(reveal.isTracking == false)
        #expect(reveal.progress == 0)
    }

    @Test
    func openDrawerClaimsHorizontalDragsWhereverTheyStart() {
        // Open, the reserved bar is part of the pushed screen beside the drawer.
        var fromBar = DrawerReveal(isOpen: true)
        let claimedFromBar = fromBar.updateDrag(
            translation: CGSize(width: -20, height: 0),
            presentedProgress: 1,
            width: width,
            startsInExcludedRegion: true
        )
        #expect(claimedFromBar)
        #expect(fromBar.isTracking)

        // A drag toward the trailing edge holds the drawer open.
        var towardOpen = DrawerReveal(isOpen: true)
        let claimedTowardOpen = towardOpen.updateDrag(
            translation: CGSize(width: 20, height: 0),
            presentedProgress: 1,
            width: width
        )
        #expect(claimedTowardOpen)
        #expect(towardOpen.isTracking)
    }

    @Test
    func closingDrawerClaimsADragTowardTheLeadingEdge() {
        var reveal = DrawerReveal(isOpen: true)
        reveal.settle(isOpen: false)

        // The closing transition is 40% on screen when the finger catches it.
        let claimed = reveal.updateDrag(translation: CGSize(width: -20, height: 0), presentedProgress: 0.4, width: width)

        #expect(claimed)
        #expect(reveal.isTracking)
        #expect(reveal.progress == 0.4)
    }

    @Test
    func requestDuringADragTakesTheRevealBackForTheRestOfThatDrag() {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: -10, height: 0), presentedProgress: 1, width: width)

        reveal.settle(isOpen: false)
        let claimed = reveal.updateDrag(translation: CGSize(width: 150, height: 0), presentedProgress: 0.5, width: width)

        // The drawer keeps the touch, so the control under the finger stays cancelled.
        #expect(claimed)
        #expect(reveal.progress == 0)
        let release = reveal.endDrag(predictedTranslation: 150, width: width)
        #expect(release == nil)
    }

    @Test
    func dragIsClampedToTheEndpoints() {
        var reveal = DrawerReveal(isOpen: true)
        reveal.updateDrag(translation: CGSize(width: 12, height: 0), presentedProgress: 1, width: width)
        reveal.updateDrag(translation: CGSize(width: 300, height: 0), presentedProgress: 1, width: width)
        #expect(reveal.progress == 1)

        reveal.updateDrag(translation: CGSize(width: -900, height: 0), presentedProgress: 1, width: width)
        #expect(reveal.progress == 0)
    }

    @Test
    func unmeasuredWidthKeepsAClaimedDragWithoutMovingTheReveal() {
        var reveal = DrawerReveal(isOpen: true)

        let claimed = reveal.updateDrag(translation: CGSize(width: -50, height: 0), presentedProgress: 1, width: 0)

        #expect(claimed)
        #expect(reveal.isTracking == false)
        #expect(reveal.progress == 1)
        #expect(reveal.endDrag(predictedTranslation: -50, width: 0) == nil)
    }

    @Test
    func swipeOpensAfterADeclinedDragFinishes() {
        var reveal = DrawerReveal(isOpen: false)
        #expect(reveal.updateDrag(translation: CGSize(width: 2, height: 24), presentedProgress: 0, width: width) == false)

        // Finishing the declined touch moves nothing, and the next touch starts fresh.
        #expect(reveal.cancelDrag() == nil)
        #expect(reveal.drag == nil)

        let claimed = reveal.updateDrag(translation: CGSize(width: 20, height: 1), presentedProgress: 0, width: width)
        #expect(claimed)
        reveal.updateDrag(translation: CGSize(width: 190, height: 3), presentedProgress: 0, width: width)
        #expect(abs(reveal.progress - 0.5) < 0.0001)
    }

    @Test
    func rightToLeftFlipsTheHorizontalAxis() {
        let screenTranslation = CGSize(width: 40, height: 5)

        #expect(
            DrawerReveal.logicalTranslation(screenTranslation, layoutDirection: .leftToRight)
                == CGSize(width: 40, height: 5)
        )
        #expect(
            DrawerReveal.logicalTranslation(screenTranslation, layoutDirection: .rightToLeft)
                == CGSize(width: -40, height: 5)
        )
    }
}

struct DrawerPhaseTests {
    @Test
    func phasesFollowThePresentedProgressAndTheModel() {
        let open = DrawerReveal(isOpen: true)
        let closed = DrawerReveal(isOpen: false)

        #expect(DrawerPhase(presentedProgress: 0, reveal: closed) == .closed)
        #expect(DrawerPhase(presentedProgress: 0.3, reveal: open) == .opening)
        #expect(DrawerPhase(presentedProgress: 1, reveal: open) == .open)
        #expect(DrawerPhase(presentedProgress: 0.7, reveal: closed) == .closing)
        #expect(DrawerPhase(presentedProgress: 0.5, reveal: DrawerReveal(heldAt: 0.5)) == .dragging)
    }

    @Test
    func onlyTheClosedEndpointReleasesTheScreen() {
        #expect(DrawerPhase.closed.isModal == false)
        #expect(DrawerPhase.opening.isModal)
        #expect(DrawerPhase.open.isModal)
        #expect(DrawerPhase.closing.isModal)
        #expect(DrawerPhase.dragging.isModal)
    }
}

struct DrawerLayoutTests {
    @Test
    func compactPhoneUsesMostOfTheWidthWithinTheBound() {
        let layout = DrawerLayout(size: CGSize(width: 375, height: 700), safeAreaInsets: EdgeInsets())

        #expect(layout.width == 375 * DrawerLayout.widthRatio)
    }

    @Test
    func widerWindowsStopAtTheReadableBound() {
        let phone = DrawerLayout(size: CGSize(width: 402, height: 800), safeAreaInsets: EdgeInsets())
        let tablet = DrawerLayout(size: CGSize(width: 1024, height: 1300), safeAreaInsets: EdgeInsets())

        #expect(phone.width == DrawerLayout.maximumWidth)
        #expect(tablet.width == DrawerLayout.maximumWidth)
    }

    @Test
    func leadingInsetWidensTheSurfaceWithoutNarrowingTheContent() {
        // Landscape iPhone: the surface extends behind the leading inset, which the content
        // keeps clear of.
        let insets = EdgeInsets(top: 0, leading: 62, bottom: 21, trailing: 62)
        let layout = DrawerLayout(size: CGSize(width: 750, height: 381), safeAreaInsets: insets)

        #expect(layout.width == DrawerLayout.maximumWidth + 62)
        #expect(layout.contentInsets.leading == 62)
        #expect(layout.contentInsets.bottom == 21)
        #expect(layout.viewportHeight == 381)
    }

    @Test
    func narrowWindowKeepsATappableStripOfTheScreen() {
        let layout = DrawerLayout(size: CGSize(width: 200, height: 600), safeAreaInsets: EdgeInsets())

        #expect(200 - layout.width >= DrawerLayout.minimumDismissalWidth)
    }
}

/// Where the tab bar's band lies, from the tab view's and a tab root's safe frames. The frames
/// are the ones measured in the harness on iPhone Air and iPad Pro 11-inch (iOS 27).
struct DrawerTabBarBandTests {
    @Test
    func bottomBarReachesDownToTheScreenEdge() throws {
        let tabView = CGRect(x: 0, y: 174, width: 420, height: 704)
        let tab = CGRect(x: 0, y: 174, width: 420, height: 655)

        let band = try #require(DrawerTabBarBand.band(tabViewSafeFrame: tabView, tabContentSafeFrame: tab))

        #expect(band.lowerBound == 829)
        // SwiftUI does not report where the floating platter ends inside the home indicator's
        // inset, so the band reaches the screen edge.
        #expect(band.contains(870))
        #expect(band.contains(800) == false)
    }

    @Test
    func topBarSpansFromTheTabViewsSafeTopToTheContent() throws {
        let tabView = CGRect(x: 0, y: 138, width: 834, height: 1047)
        let tab = CGRect(x: 0, y: 202, width: 834, height: 983)

        let band = try #require(DrawerTabBarBand.band(tabViewSafeFrame: tabView, tabContentSafeFrame: tab))

        #expect(band == 138...202)
        // The home indicator strip at the bottom stays open to drawer drags.
        #expect(band.contains(1197) == false)
    }

    @Test
    func noBandWithoutABarOrBeforeLayout() {
        let frame = CGRect(x: 0, y: 100, width: 400, height: 700)

        #expect(DrawerTabBarBand.band(tabViewSafeFrame: frame, tabContentSafeFrame: frame) == nil)
        #expect(DrawerTabBarBand.band(tabViewSafeFrame: .zero, tabContentSafeFrame: frame) == nil)
    }
}
