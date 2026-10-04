import SwiftUI
import Testing
import UIKit
@testable import UI

/// A full-list state fills the visible height left below the content above it, as a minimum:
/// a list with a header is no taller than one without, and a state taller than the space left
/// grows the list instead of being clipped. The visible height is the list's own, measured
/// without the bars and safe areas that overlap it.
@MainActor
struct FullListStateLayoutTests {
    @Test
    func withoutContentAboveTheStateFillsTheMinimumHeight() {
        let height = fittingHeight {
            FullListStateLayout(minHeight: 600) {
                state(idealHeight: 100)
            }
        }

        #expect(height == 600)
    }

    @Test
    func contentAboveTakesItsHeightFromTheStateSoTheTotalStaysTheMinimum() {
        let height = fittingHeight {
            FullListStateLayout(minHeight: 600) {
                Color.clear.frame(height: 150)
                state(idealHeight: 100)
            }
        }

        #expect(height == 600)
    }

    @Test
    func aStateTallerThanTheSpaceLeftGrowsTheLayout() {
        let height = fittingHeight {
            FullListStateLayout(minHeight: 600) {
                Color.clear.frame(height: 150)
                state(idealHeight: 500)
            }
        }

        #expect(height == 650)
    }

    @Test
    func contentAboveTallerThanTheMinimumLeavesTheStateItsIdealHeight() {
        let height = fittingHeight {
            FullListStateLayout(minHeight: 600) {
                Color.clear.frame(height: 700)
                state(idealHeight: 100)
            }
        }

        #expect(height == 800)
    }

    @Test
    func theStateIsCenteredInTheSpaceLeftBelowTheContentAbove() {
        let probe = StateFrameProbe()
        let host = UIHostingController(
            rootView: FullListStateLayout(minHeight: 600) {
                Color.clear.frame(height: 200)
                Color.clear
                    .frame(height: 100)
                    .background(GeometryReader { proxy in
                        Color.clear.onAppear { probe.frame = proxy.frame(in: .named("layout")) }
                    })
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .coordinateSpace(name: "layout")
            .frame(width: 300, height: 600)
        )
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 300, height: 600))
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.layoutIfNeeded()
        RunLoop.main.run(until: Date().addingTimeInterval(0.1))

        // The 400 points below the content above have the 100-point state in their middle.
        #expect(probe.frame.minY == 350)
        window.isHidden = true
    }

    @Test
    func theViewportCentersTheStateInTheVisibleSpaceAboveABottomBar() {
        let probe = StateFrameProbe()
        let insets = EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0)
        let (window, host) = show(
            ScrollView {
                ViewportStateList(headerHeight: 100, probe: probe)
                    .padding(insets)
            }
            .fullListStateViewport(contentInsets: insets)
            // Stands in for a tab bar: the list scrolls beneath it, so it is not visible space.
            .safeAreaInset(edge: .bottom, spacing: 0) {
                Color.clear.frame(height: 100)
            }
        )

        // Visible: 700 - 100 = 600. Space for the header and state: 600 - 16 = 584, so the state's
        // area runs from 8 + 100 = 108 to 8 + 584 = 592, and the 100-point state starts at 300.
        #expect(probe.frame.minY == 300)
        #expect(host.view.frame.height == 700)
        window.isHidden = true
    }

    @Test
    func withoutAMeasuredViewportTheStateTakesItsIdealHeight() {
        let height = fittingHeight {
            ViewportStateList(headerHeight: 100, probe: StateFrameProbe())
        }

        #expect(height == 200)
    }

    private func show<Content: View>(_ content: Content) -> (UIWindow, UIHostingController<some View>) {
        let host = UIHostingController(rootView: content.frame(width: 300, height: 700))
        if #available(iOS 16.4, *) {
            host.safeAreaRegions = []
        }
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 300, height: 700))
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.layoutIfNeeded()
        RunLoop.main.run(until: Date().addingTimeInterval(0.3))
        return (window, host)
    }

    private func state(idealHeight: CGFloat) -> some View {
        Color.clear
            .frame(height: idealHeight)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func fittingHeight<Content: View>(@ViewBuilder _ content: () -> Content) -> CGFloat {
        UIHostingController(rootView: content())
            .sizeThatFits(in: CGSize(width: 300, height: CGFloat.greatestFiniteMagnitude))
            .height
    }
}

@MainActor
private final class StateFrameProbe {
    var frame: CGRect = .zero
}

/// A header and a 100-point state laid out as the paging states lay them out, reporting the
/// state's frame in the window.
private struct ViewportStateList: View {
    let headerHeight: CGFloat
    let probe: StateFrameProbe

    @Environment(\.fullListStateHeight) private var fullListStateHeight

    var body: some View {
        FullListStateLayout(minHeight: fullListStateHeight) {
            Color.clear.frame(height: headerHeight)
            Color.clear
                .frame(height: 100)
                .background(GeometryReader { proxy in
                    Color.clear.preference(key: StateFrameKey.self, value: proxy.frame(in: .global))
                })
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onPreferenceChange(StateFrameKey.self) { frame in
            probe.frame = frame
        }
    }
}

private struct StateFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero

    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        value = nextValue()
    }
}
