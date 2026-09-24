import SwiftUI
import Testing
import ViewInspector
@testable import UI

/// Rendered interaction boundaries of `drawer(isShowing:content:)` at its closed endpoint, open
/// endpoint, and mid-transition.
///
/// ViewInspector's `tap()` refuses a control with a disabled, hidden, or non-hit-testable
/// ancestor, so a throwing tap is the rendered equivalent of a touch that cannot reach the
/// control. ViewInspector 0.10.3 or newer is required for these assertions on iOS 26 runtimes.
@MainActor
struct DrawerInteractionTests {
    @Test
    func closedDrawerLeavesTheScreenInteractiveAndItsActionsUnreachable() throws {
        let recorder = Recorder(isShowing: false)
        let drawer = recorder.drawer()

        try drawer.inspect().find(button: screenActionTitle).tap()
        #expect(recorder.screenActions == 1)

        let profile = try drawer.inspect().find(button: drawerActionTitle)
        #expect(throws: InspectionError.self) { try profile.tap() }
        #expect(isHiddenFromAccessibility(profile))
        #expect(throws: InspectionError.self) {
            try drawer.inspect().find(viewWithAccessibilityLabel: dismissalLabel)
        }
    }

    @Test
    func openDrawerBlocksTheScreenAndExposesItsActionsAndDismissal() throws {
        let recorder = Recorder(isShowing: true)
        let drawer = recorder.drawer()

        let screenAction = try drawer.inspect().find(button: screenActionTitle)
        #expect(throws: InspectionError.self) { try screenAction.tap() }
        #expect(isHiddenFromAccessibility(screenAction))

        let profile = try drawer.inspect().find(button: drawerActionTitle)
        #expect(isHiddenFromAccessibility(profile) == false)
        _ = try drawer.inspect().find(viewWithAccessibilityLabel: dismissalLabel)
        #expect(recorder.screenActions == 0)
    }

    @Test
    func tappingThePushedScreenClosesTheDrawerWithoutReachingItsControls() throws {
        let recorder = Recorder(isShowing: true)
        let drawer = recorder.drawer()

        try drawer.inspect().find(viewWithAccessibilityLabel: dismissalLabel).button().tap()

        #expect(recorder.isShowing == false)
        #expect(recorder.screenActions == 0)
    }

    @Test
    func midTransitionBlocksBothTheScreenAndTheDrawerActions() throws {
        let recorder = Recorder(isShowing: true)
        let drawer = DrawerContainer(
            isShowing: recorder.binding,
            base: recorder.screen,
            drawerContent: { recorder.menu },
            reveal: DrawerReveal(heldAt: 0.5)
        )

        let screenAction = try drawer.inspect().find(button: screenActionTitle)
        #expect(throws: InspectionError.self) { try screenAction.tap() }
        #expect(isHiddenFromAccessibility(screenAction))

        let profile = try drawer.inspect().find(button: drawerActionTitle)
        #expect(throws: InspectionError.self) { try profile.tap() }
        #expect(isHiddenFromAccessibility(profile))

        // The dismissal surface stays available while the drawer is in motion.
        _ = try drawer.inspect().find(viewWithAccessibilityLabel: dismissalLabel)
    }

    @Test
    func choosingADestinationDispatchesItOnceAndClosesTheDrawer() throws {
        let recorder = Recorder(isShowing: true)
        let drawer = recorder.drawer()

        try drawer.inspect().find(button: drawerActionTitle).tap()

        #expect(recorder.destinationDispatches == 1)
        #expect(recorder.isShowing == false)
        #expect(recorder.screenActions == 0)
    }
}

extension DrawerInteractionTests {
    /// The drawer's drag as the container attaches it: its mask, and the decision each drag
    /// update makes through the container's handler and the reveal model.
    ///
    /// `callUpdating` feeds the gesture a synthesized value, so these tests cover wiring and
    /// decisions, not arbitration with other gestures; `DrawerPassUITests` covers that with real
    /// drags. The container's state is not hosted here, so each call starts from the reveal the
    /// container was created with. Nested in `DrawerInteractionTests`, which has the same
    /// simulator-runtime constraint, so skipping that suite skips this one too.
    @MainActor
    struct DragWiring {
        @Test(arguments: DrawerStateAtDestination.allCases)
        func aDestinationDetachesTheDragInEveryDrawerState(state: DrawerStateAtDestination) throws {
            let recorder = Recorder(isShowing: state.isShowing)

            let host = containerDrag(recorder: recorder, reveal: state.reveal, allowsDragging: true)
            let destination = containerDrag(recorder: recorder, reveal: state.reveal, allowsDragging: false)

            #expect(try host().gestureMask() == .all)
            #expect(try destination().gestureMask() == .subviews)
        }

        @Test
        func cancellingAGrabbedOpeningReturnsTheBindingToClosed() throws {
            // Closed and settled, the opener requested an opening; a finger caught it at 30% and
            // pulled it back to about 10% when the system cancelled the touch.
            var reveal = DrawerReveal(isOpen: false)
            reveal.settle(isOpen: true)
            reveal.updateDrag(translation: CGSize(width: -20, height: 0), presentedProgress: 0.3, width: 340)
            reveal.updateDrag(translation: CGSize(width: -88, height: 0), presentedProgress: 0.3, width: 340)
            let recorder = Recorder(isShowing: true)

            try dragHandler(recorder: recorder, reveal: reveal).onFinish()

            #expect(recorder.isShowing == false)
            #expect(recorder.destinationDispatches == 0)
            #expect(recorder.screenActions == 0)
        }

        @Test
        func cancellingAGrabbedClosingReturnsTheBindingToOpen() throws {
            var reveal = DrawerReveal(isOpen: true)
            reveal.settle(isOpen: false)
            reveal.updateDrag(translation: CGSize(width: 20, height: 0), presentedProgress: 0.7, width: 340)
            let recorder = Recorder(isShowing: false)

            try dragHandler(recorder: recorder, reveal: reveal).onFinish()

            #expect(recorder.isShowing == true)
        }

        @Test
        func aDragAnEarlierTouchLeftBehindDoesNotDecideTheNextTouch() throws {
            // A quick vertical flick was declined and lifted before a render observed its end, so
            // nothing finished it.
            var leftover = DrawerReveal(isOpen: false)
            leftover.updateDrag(translation: CGSize(width: 2, height: 24), presentedProgress: 0, width: 340)
            let recorder = Recorder(isShowing: false)
            let drag = containerDrag(recorder: recorder, reveal: leftover)

            // A leading swipe is declined, so the row under it keeps its touch…
            let leading = try update(drag(), from: CGPoint(x: 300, y: 300), by: CGSize(width: -24, height: 1))
            #expect(leading.isDeclined)

            // …and a trailing swipe is claimed and opens the drawer.
            let trailing = try update(drag(), from: CGPoint(x: 60, y: 300), by: CGSize(width: 24, height: 1))
            #expect(trailing.isDeclined == false)
            #expect(recorder.isShowing == false)
        }

        @Test
        func closedDrawerClaimsATrailingDragAndDeclinesALeadingOne() throws {
            let recorder = Recorder(isShowing: false)
            let drag = containerDrag(recorder: recorder)

            let trailing = try update(drag(), from: CGPoint(x: 60, y: 300), by: CGSize(width: 24, height: 1))
            #expect(trailing.isActive)
            #expect(trailing.isDeclined == false)

            let leading = try update(drag(), from: CGPoint(x: 300, y: 300), by: CGSize(width: -24, height: 1))
            #expect(leading.isDeclined)
        }

        @Test
        func closedDrawerDeclinesDragsThatStartOnTheReportedTabBar() throws {
            let recorder = Recorder(isShowing: false)
            let drag = containerDrag(recorder: recorder, excludedBand: 780...CGFloat.greatestFiniteMagnitude)

            let onBar = try update(drag(), from: CGPoint(x: 60, y: 800), by: CGSize(width: 24, height: 0))
            #expect(onBar.isDeclined)

            let aboveBar = try update(drag(), from: CGPoint(x: 60, y: 760), by: CGSize(width: 24, height: 0))
            #expect(aboveBar.isDeclined == false)
        }

        @Test
        func openDrawerClaimsDragsThatStartWhereTheTabBarWas() throws {
            let recorder = Recorder(isShowing: true)
            let drag = containerDrag(recorder: recorder, excludedBand: 780...CGFloat.greatestFiniteMagnitude)

            let onStrip = try update(drag(), from: CGPoint(x: 380, y: 800), by: CGSize(width: -24, height: 0))
            #expect(onStrip.isDeclined == false)
        }

        /// The tab-bar modifiers report the band in the global coordinates the drag's start
        /// location uses: a hosted layout's bar begins exactly where the reported band does.
        @Test
        func tabBarBandIsReportedWhereTheBarBeginsInGlobalCoordinates() async throws {
            let probe = BandProbe()
            let view = Color.clear
                .drawerTabBarBoundary()
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    Color.clear
                        .frame(height: 60)
                        .background {
                            GeometryReader { geometry in
                                Color.clear.onAppear { probe.barTop = geometry.frame(in: .global).minY }
                            }
                        }
                }
                .excludesTabBarFromDrawerDrags()
                .environment(\.drawerExcludedBandReporter, DrawerExcludedBandReporter { probe.band = $0 })
                .frame(width: 320, height: 640)

            ViewHosting.host(view: view)
            defer { ViewHosting.expel() }
            try await Task.sleep(nanoseconds: 500_000_000)

            let band = try #require(probe.band)
            let barTop = try #require(probe.barTop)
            #expect(abs(band.lowerBound - barTop) < 0.5)
        }

        private func container(
            recorder: Recorder,
            reveal: DrawerReveal?,
            allowsDragging: Bool,
            excludedBand: ClosedRange<CGFloat>?
        ) -> some View {
            DrawerContainer(
                isShowing: recorder.binding,
                allowsDragging: allowsDragging,
                base: recorder.screen,
                drawerContent: { recorder.menu },
                reveal: reveal,
                excludedBand: excludedBand
            )
        }

        private func containerDrag(
            recorder: Recorder,
            reveal: DrawerReveal? = nil,
            allowsDragging: Bool = true,
            excludedBand: ClosedRange<CGFloat>? = nil
        ) -> () throws -> InspectableView<ViewType.Gesture<DragGesture>> {
            let view = container(
                recorder: recorder,
                reveal: reveal,
                allowsDragging: allowsDragging,
                excludedBand: excludedBand
            )
            return {
                try view.inspect()
                    .find(ViewType.ZStack.self)
                    .modifier(DrawerDragGesture.self)
                    .viewModifierContent()
                    .highPriorityGesture(DragGesture.self)
            }
        }

        /// The handler the container gives its drag, as the gesture's end-of-touch paths call it.
        private func dragHandler(recorder: Recorder, reveal: DrawerReveal) throws -> DrawerDragHandler {
            try container(recorder: recorder, reveal: reveal, allowsDragging: true, excludedBand: nil)
                .inspect()
                .find(ViewType.ZStack.self)
                .modifier(DrawerDragGesture.self)
                .actualView()
                .handler
        }

        /// Feeds one drag update to the gesture and returns the touch state it leaves.
        private func update(
            _ gesture: InspectableView<ViewType.Gesture<DragGesture>>,
            from start: CGPoint,
            by translation: CGSize
        ) throws -> DrawerTouch {
            var touch = DrawerTouch()
            var transaction = Transaction()
            let value = DragGesture.Value(
                time: Date(),
                location: CGPoint(x: start.x + translation.width, y: start.y + translation.height),
                startLocation: start,
                velocity: .zero
            )
            try gesture.callUpdating(value: value, state: &touch, transaction: &transaction)
            return touch
        }
    }
}

extension DrawerInteractionTests {
    /// A host wired the way both routers are: its own row opens a destination through
    /// `DrawerHostNavigation.open` and its menu control opens the drawer through `toggleDrawer`,
    /// and the drawer's rows leave through `openFromDrawer` and `closeDrawerForChoice`.
    /// ViewInspector activates the rendered controls with no render in
    /// between, as two activations before SwiftUI's next update arrive: the drawer still shows its
    /// rows as open and responsive when the second one lands. Nested in `DrawerInteractionTests`
    /// for its simulator-runtime constraint.
    @MainActor
    struct HostNavigationWiring {
        @Test
        func aDrawerDestinationActivatedTwiceBeforeTheNextUpdateOpensOnce() throws {
            let host = NavigationRecorder(isDrawerOpen: true)
            let profile = try host.view().inspect().find(button: drawerActionTitle)

            try profile.tap()
            try profile.tap()

            #expect(host.navigation.path == NavigationPath([HostRoute.profile]))
            #expect(host.navigation.isDrawerOpen == false)
        }

        @Test
        func twoDrawerRowsActivatedBeforeTheNextUpdateRunOnlyTheFirst() throws {
            let host = NavigationRecorder(isDrawerOpen: true)
            let view = host.view()
            let profile = try view.inspect().find(button: drawerActionTitle)
            let signOut = try view.inspect().find(button: signOutTitle)

            try profile.tap()
            try signOut.tap()

            #expect(host.navigation.path == NavigationPath([HostRoute.profile]))
            #expect(host.signOuts == 0)
        }

        @Test
        func aHostRowActivatedTwiceBeforeTheNextUpdateOpensOnce() throws {
            let host = NavigationRecorder(isDrawerOpen: false)
            let row = try host.view().inspect().find(button: screenActionTitle)

            try row.tap()
            try row.tap()

            #expect(host.navigation.path == NavigationPath([HostRoute.gambler]))
        }

        @Test
        func aHostRowAndTheMenuControlActivatedBeforeTheNextUpdateLeaveTheDrawerClosed() throws {
            let host = NavigationRecorder(isDrawerOpen: false)
            let view = host.view()
            let row = try view.inspect().find(button: screenActionTitle)
            let opener = try view.inspect().find(button: openerTitle)

            try row.tap()
            try opener.tap()

            #expect(host.navigation.path == NavigationPath([HostRoute.gambler]))
            #expect(host.navigation.isDrawerOpen == false)
        }

        @Test
        func laterChoicesOpenOnceTheHostIsBackAndTheDrawerReopens() throws {
            let host = NavigationRecorder(isDrawerOpen: true)
            try host.view().inspect().find(button: drawerActionTitle).tap()

            // Back, then the opener.
            host.navigation.path.removeLast()
            host.navigation.isDrawerOpen = true
            try host.view().inspect().find(button: signOutTitle).tap()
            #expect(host.signOuts == 1)

            // The drawer closed for signing out; the host's row still opens its destination.
            try host.view().inspect().find(button: screenActionTitle).tap()
            #expect(host.navigation.path == NavigationPath([HostRoute.gambler]))
        }
    }
}

/// Where the drawer can be when a host shows a destination: resting closed or open, or still
/// closing after the destination was chosen from the drawer.
enum DrawerStateAtDestination: CaseIterable, Sendable {
    case restingClosed
    case restingOpen
    case closingAfterChoice

    var isShowing: Bool {
        self == .restingOpen
    }

    var reveal: DrawerReveal {
        switch self {
        case .restingClosed:
            return DrawerReveal(isOpen: false)
        case .restingOpen:
            return DrawerReveal(isOpen: true)
        case .closingAfterChoice:
            var reveal = DrawerReveal(isOpen: true)
            reveal.settle(isOpen: false)
            return reveal
        }
    }
}

@MainActor
private final class BandProbe {
    var band: ClosedRange<CGFloat>?
    var barTop: CGFloat?
}

private let screenActionTitle = "Screen action"
private let drawerActionTitle = "Profile"
private let dismissalLabel = "Close menu"

/// Stands in for a host: a screen control, and a drawer destination that closes the drawer and
/// navigates, the way both routers' callbacks do.
@MainActor
private final class Recorder {
    var isShowing: Bool
    var screenActions = 0
    var destinationDispatches = 0

    init(isShowing: Bool) {
        self.isShowing = isShowing
    }

    var binding: Binding<Bool> {
        Binding(get: { self.isShowing }, set: { self.isShowing = $0 })
    }

    var screen: some View {
        Button(screenActionTitle) { self.screenActions += 1 }
    }

    var menu: some View {
        Button(drawerActionTitle) {
            self.isShowing = false
            self.destinationDispatches += 1
        }
    }

    func drawer() -> some View {
        screen.drawer(isShowing: binding) { self.menu }
    }
}

private let signOutTitle = "Sign out"
private let openerTitle = "Open menu"

private enum HostRoute: Hashable {
    case profile
    case gambler
}

/// Stands in for a router: a screen row and two drawer rows, one that opens a destination and
/// one that leaves without one, all through the host's `DrawerHostNavigation`.
@MainActor
private final class NavigationRecorder {
    var navigation = DrawerHostNavigation()
    var signOuts = 0

    init(isDrawerOpen: Bool) {
        navigation.isDrawerOpen = isDrawerOpen
    }

    func view() -> some View {
        VStack {
            Button(openerTitle) { self.navigation.toggleDrawer() }
            Button(screenActionTitle) { self.navigation.open(HostRoute.gambler) }
        }
            .drawer(
                isShowing: Binding(get: { self.navigation.isDrawerOpen }, set: { self.navigation.isDrawerOpen = $0 }),
                allowsDragging: navigation.isHostVisible
            ) {
                VStack {
                    Button(drawerActionTitle) { self.navigation.openFromDrawer(HostRoute.profile) }
                    Button(signOutTitle) {
                        if self.navigation.closeDrawerForChoice() {
                            self.signOuts += 1
                        }
                    }
                }
            }
    }
}

/// Whether the view or any ancestor is hidden from assistive technology.
private func isHiddenFromAccessibility<V>(_ view: InspectableView<V>) -> Bool {
    if (try? view.accessibilityHidden()) == true {
        return true
    }
    guard let parent = try? view.parent() else { return false }
    return isHiddenFromAccessibility(parent)
}
