import SwiftUI
import Testing
import UIKit
@testable import UI

/// Standalone glass actions (and their bordered fallbacks below iOS 26) need at least a
/// 44 × 44-point touch target at every text size. The button is laid out by SwiftUI in a hosting
/// controller without safe areas, so the measured size is the control's own.
@MainActor
struct LiquidGlassButtonSizeTests {
    private static let minimumTarget: CGFloat = 44

    @available(iOS 16.4, *)
    @Test(arguments: DynamicTypeSize.allCases)
    func glassActionsMeetTheMinimumTouchTarget(size: DynamicTypeSize) {
        let styled: [(String, AnyView)] = [
            ("liquidGlass", AnyView(Button("OK") {}.buttonStyle(.liquidGlass))),
            ("liquidGlassProminent", AnyView(Button("OK") {}.buttonStyle(.liquidGlassProminent))),
        ]
        for (name, button) in styled {
            let measured = fittingSize(of: button.environment(\.dynamicTypeSize, size))
            #expect(measured.height >= Self.minimumTarget, "\(name) at \(size): \(measured)")
            #expect(measured.width >= Self.minimumTarget, "\(name) at \(size): \(measured)")
        }
    }

    @available(iOS 16.4, *)
    private func fittingSize(of view: some View) -> CGSize {
        let host = UIHostingController(rootView: view)
        host.safeAreaRegions = []
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
        window.rootViewController = host
        window.makeKeyAndVisible()
        defer { window.isHidden = true }
        return host.sizeThatFits(in: CGSize(width: 370, height: 800))
    }
}
