import SwiftUI
import Testing
import UIKit
@testable import UI

/// Accent-filled prominent actions draw their label in `onPrimary`: white on the light accent and
/// black on the dark accent. The secondary glass style's pre-26 fallback draws a primary-color
/// label rather than accent text. The package has no app accent, so each case tints the button with the
/// app's `AccentColor` value for that appearance (#2E7D32 light, #4CAF50 dark). The button is laid
/// out in a window, its layer is rendered, and the label is measured against the rendered fill.
@MainActor
struct AccentFilledLabelTests {
    private static let lightAccent = UIColor(red: 0x2E / 255, green: 0x7D / 255, blue: 0x32 / 255, alpha: 1)
    private static let darkAccent = UIColor(red: 0x4C / 255, green: 0xAF / 255, blue: 0x50 / 255, alpha: 1)

    struct Case: CustomTestStringConvertible, Sendable {
        let style: UIUserInterfaceStyle
        let contrast: UIAccessibilityContrast
        var testDescription: String { "\(style == .dark ? "dark" : "light"), \(contrast == .high ? "high contrast" : "normal contrast")" }
    }

    nonisolated private static let cases: [Case] = [UIUserInterfaceStyle.light, .dark].flatMap { style in
        [UIAccessibilityContrast.normal, .high].map { Case(style: style, contrast: $0) }
    }

    @available(iOS 17.0, *)
    @Test(arguments: cases)
    func standardProminentLabelIsLegibleOnTheAccent(_ testCase: Case) throws {
        let pixels = try render(Button("WWWW") {}.buttonStyle(.standardProminent), testCase)
        #expect(labelContrast(pixels) >= 4.5, "\(testCase.testDescription)")
    }

    /// Liquid Glass is not drawn by an offscreen layer render, so this covers the bordered branch
    /// used below iOS 26; the glass branch is checked on screen.
    @available(iOS 17.0, *)
    @Test(.enabled(if: !glassIsAvailable), arguments: cases)
    func liquidGlassProminentLabelIsLegibleOnTheAccent(_ testCase: Case) throws {
        let pixels = try render(Button("WWWW") {}.buttonStyle(.liquidGlassProminent), testCase)
        #expect(labelContrast(pixels) >= 4.5, "\(testCase.testDescription)")
    }

    /// The secondary style's `.bordered` fallback (below iOS 26) would draw accent text on its gray
    /// fill; it uses the primary color instead, even under the app accent tint set by `render`.
    @available(iOS 17.0, *)
    @Test(.enabled(if: !glassIsAvailable), arguments: cases)
    func liquidGlassSecondaryFallbackLabelIsLegible(_ testCase: Case) throws {
        let pixels = try render(Button("WWWW") {}.buttonStyle(.liquidGlass), testCase)
        #expect(labelContrast(pixels) >= 4.5, "\(testCase.testDescription)")
    }

    /// The create-pool toolbar circle below iOS 26: the add glyph at its toolbar size, on the
    /// accent-filled 44-point circle. On iOS 26 the style draws Liquid Glass instead, which is
    /// checked on screen.
    @available(iOS 17.0, *)
    @Test(.enabled(if: !glassIsAvailable), arguments: cases)
    func toolbarProminentFallbackGlyphIsLegibleOnTheAccent(_ testCase: Case) throws {
        let glyph = Image(sharedResource: .add)
            .resizable()
            .frame(width: ToolbarProminentButtonStyle.glyphSize, height: ToolbarProminentButtonStyle.glyphSize)
        let pixels = try render(Button {} label: { glyph }.buttonStyle(.toolbarProminent), testCase)
        #expect(labelContrast(pixels) >= 4.5, "\(testCase.testDescription)")
    }

    /// The label color applies only while enabled: a disabled action looks exactly like the
    /// disabled system style it is built on.
    @available(iOS 17.0, *)
    @Test(arguments: cases)
    func disabledStandardProminentKeepsTheSystemPresentation(_ testCase: Case) throws {
        let styled = try render(Button("WWWW") {}.buttonStyle(.standardProminent).disabled(true), testCase)
        let system = try render(Button("WWWW") {}.buttonStyle(.borderedProminent).disabled(true), testCase)
        #expect(styled == system)
    }

    @available(iOS 17.0, *)
    @Test(.enabled(if: !glassIsAvailable), arguments: cases)
    func disabledLiquidGlassProminentKeepsTheSystemPresentation(_ testCase: Case) throws {
        let styled = try render(Button("WWWW") {}.buttonStyle(.liquidGlassProminent).disabled(true), testCase)
        let system = try render(
            Button("WWWW") {}.buttonStyle(.borderedProminent).controlSize(.large).disabled(true),
            testCase
        )
        #expect(styled == system)
    }

    @available(iOS 17.0, *)
    private func render(_ button: some View, _ testCase: Case) throws -> [Pixel] {
        let accent = testCase.style == .dark ? Self.darkAccent : Self.lightAccent
        let host = UIHostingController(rootView: button.font(.body.weight(.bold)).tint(Color(uiColor: accent)))
        host.safeAreaRegions = []
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
        window.overrideUserInterfaceStyle = testCase.style
        window.traitOverrides.accessibilityContrast = testCase.contrast
        window.rootViewController = host
        window.makeKeyAndVisible()
        defer { window.isHidden = true }

        let size = host.sizeThatFits(in: CGSize(width: 370, height: 800))
        host.view.frame = CGRect(origin: .zero, size: size)
        host.view.layoutIfNeeded()
        // Let SwiftUI commit the frame before drawing it.
        RunLoop.main.run(until: Date().addingTimeInterval(0.2))

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.preferredRange = .standard
        let image = UIGraphicsImageRenderer(size: size, format: format).image { context in
            host.view.layer.render(in: context.cgContext)
        }
        let cgImage = try #require(image.cgImage)
        return centralPixels(of: cgImage)
    }
}

private let glassIsAvailable = ProcessInfo.processInfo.isOperatingSystemAtLeast(
    OperatingSystemVersion(majorVersion: 26, minorVersion: 0, patchVersion: 0)
)

private struct Pixel: Hashable {
    let red: UInt8
    let green: UInt8
    let blue: UInt8
}

/// Pixels in the middle of the control (the inner 70 % horizontally and 50 % vertically), away
/// from the rounded corners, where only the fill and the label are drawn.
private func centralPixels(of image: CGImage) -> [Pixel] {
    let width = image.width
    let height = image.height
    var data = [UInt8](repeating: 0, count: width * height * 4)
    let context = CGContext(
        data: &data,
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: width * 4,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    )!
    context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
    var pixels: [Pixel] = []
    for y in (height / 4)..<(height * 3 / 4) {
        for x in (width * 15 / 100)..<(width * 85 / 100) {
            let offset = (y * width + x) * 4
            pixels.append(Pixel(red: data[offset], green: data[offset + 1], blue: data[offset + 2]))
        }
    }
    return pixels
}

/// The fill is the most frequent color; the label is the drawn color that differs most from it.
private func labelContrast(_ pixels: [Pixel]) -> Double {
    let counts = Dictionary(pixels.map { ($0, 1) }, uniquingKeysWith: +)
    guard let fill = counts.max(by: { $0.value < $1.value })?.key else { return 0 }
    return counts.keys.map { contrast($0, fill) }.max() ?? 0
}

private func contrast(_ first: Pixel, _ second: Pixel) -> Double {
    let firstLuminance = relativeLuminance(first)
    let secondLuminance = relativeLuminance(second)
    return (max(firstLuminance, secondLuminance) + 0.05) / (min(firstLuminance, secondLuminance) + 0.05)
}

private func relativeLuminance(_ pixel: Pixel) -> Double {
    func channel(_ value: UInt8) -> Double {
        let normalized = Double(value) / 255
        return normalized <= 0.03928 ? normalized / 12.92 : pow((normalized + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(pixel.red) + 0.7152 * channel(pixel.green) + 0.0722 * channel(pixel.blue)
}
