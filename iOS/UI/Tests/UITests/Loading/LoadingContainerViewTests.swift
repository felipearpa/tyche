import SwiftUI
import Testing
import UIKit
import ViewInspector
@testable import UI

/// The loading host's contract: while loading, the covered screen cannot be activated and is
/// hidden from assistive technology behind one loading status; when loading ends, the same
/// screen, not a rebuilt one, is interactive again. The spoken status is the English literal, so
/// these need an English-language simulator.
@MainActor
struct LoadingContainerViewTests {
    @Test
    func loadingBlocksTheCoveredControlAndExposesOneStatus() throws {
        let recorder = ActionRecorder()
        let host = LoadingContainerView(isLoading: true) { recorder.screen() }

        let action = try host.inspect().find(button: screenActionTitle)
        #expect(throws: InspectionError.self) { try action.tap() }
        #expect(isHiddenFromAccessibility(action))
        #expect(recorder.actions == 0)

        let status = try host.inspect().find(viewWithAccessibilityLabel: loadingStatus)
        #expect(isHiddenFromAccessibility(status) == false)
    }

    @Test
    func loadingTakesTouchesAwayFromTheCoveredScreen() {
        let state = LoadingState()
        let probe = UIView()
        let window = show(LoadingContainerHost(state: state) { ViewProbe(view: probe) })
        defer { window.isHidden = true }
        let center = probe.convert(CGPoint(x: probe.bounds.midX, y: probe.bounds.midY), to: window)

        #expect(window.hitTest(center, with: nil) === probe)

        state.isLoading = true
        settle()
        #expect(window.hitTest(center, with: nil) !== probe)

        state.isLoading = false
        settle()
        #expect(window.hitTest(center, with: nil) === probe)
    }

    @Test
    func idleHostLeavesTheScreenInteractiveWithoutAStatus() throws {
        let recorder = ActionRecorder()
        let host = LoadingContainerView(isLoading: false) { recorder.screen() }

        let action = try host.inspect().find(button: screenActionTitle)
        try action.tap()
        #expect(recorder.actions == 1)
        #expect(isHiddenFromAccessibility(action) == false)
        #expect(throws: InspectionError.self) {
            try host.inspect().find(viewWithAccessibilityLabel: loadingStatus)
        }
    }

    @Test
    func aHostWithoutContentShowsOnlyTheStatus() throws {
        let host = LoadingContainerView(isLoading: true)

        _ = try host.inspect().find(viewWithAccessibilityLabel: loadingStatus)
    }

    @Test
    func startingAndEndingLoadingKeepsTheSameScreen() {
        let lifecycle = ScreenLifecycle()
        let state = LoadingState()
        let window = show(LoadingStateHost(state: state, lifecycle: lifecycle))

        state.isLoading = true
        settle()
        state.isLoading = false
        settle()
        state.isLoading = true
        settle()

        // A screen rebuilt for a loading branch would create its state again and reappear.
        #expect(lifecycle.creations == 1)
        #expect(lifecycle.appearances == 1)
        #expect(lifecycle.disappearances == 0)
        window.isHidden = true
    }

    private func show<Content: View>(_ content: Content) -> UIWindow {
        let host = UIHostingController(rootView: content)
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.layoutIfNeeded()
        settle()
        return window
    }

    private func settle() {
        RunLoop.main.run(until: Date().addingTimeInterval(0.2))
    }
}

/// The ball turns linearly through a full circle every two seconds and never reverses.
struct BallSpinnerRotationTests {
    @Test(arguments: [(0.0, 0.0), (0.5, 90.0), (1.0, 180.0), (1.5, 270.0), (2.0, 0.0), (2.25, 45.0)])
    func angleGrowsLinearlyAndWrapsEveryTwoSeconds(seconds: TimeInterval, degrees: Double) {
        let date = Date(timeIntervalSinceReferenceDate: 1_000 + seconds)

        #expect(abs(BallSpinner.rotation(at: date).degrees - degrees) < 0.0001)
    }
}

private let screenActionTitle = "Screen action"
private let loadingStatus = "Loading"

@MainActor
private final class ActionRecorder {
    var actions = 0

    func screen() -> some View {
        Button(screenActionTitle) { self.actions += 1 }
    }
}

@MainActor
private final class LoadingState: ObservableObject {
    @Published var isLoading = false
}

/// Counts how often the screen behind the host is created, appears, and disappears.
@MainActor
private final class ScreenLifecycle {
    var creations = 0
    var appearances = 0
    var disappearances = 0
}

@MainActor
private final class ScreenModel: ObservableObject {
    init(lifecycle: ScreenLifecycle) {
        lifecycle.creations += 1
    }
}

/// Hosts arbitrary content in a loading host driven by `state`.
private struct LoadingContainerHost<Content: View>: View {
    @ObservedObject var state: LoadingState
    @ViewBuilder let content: () -> Content

    var body: some View {
        LoadingContainerView(isLoading: state.isLoading, content: content)
    }
}

/// Places a given UIKit view in the screen, so tests can hit-test it and drive its responder state.
private struct ViewProbe: UIViewRepresentable {
    let view: UIView

    func makeUIView(context: Context) -> UIView { view }
    func updateUIView(_ uiView: UIView, context: Context) {}
}

private struct LoadingStateHost: View {
    @ObservedObject var state: LoadingState
    let lifecycle: ScreenLifecycle

    var body: some View {
        LoadingContainerView(isLoading: state.isLoading) {
            TrackedScreen(lifecycle: lifecycle)
        }
    }
}

private struct TrackedScreen: View {
    let lifecycle: ScreenLifecycle
    @StateObject private var model: ScreenModel

    init(lifecycle: ScreenLifecycle) {
        self.lifecycle = lifecycle
        _model = StateObject(wrappedValue: ScreenModel(lifecycle: lifecycle))
    }

    var body: some View {
        Text("Screen")
            .onAppear { lifecycle.appearances += 1 }
            .onDisappear { lifecycle.disappearances += 1 }
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
