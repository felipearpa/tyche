import SwiftUI

struct EmailAvatar: View {
    let email: String

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        GeometryReader { proxy in
            let diameter = min(proxy.size.width, proxy.size.height)
            if let initial = email.avatarInitial {
                let (background, foreground) = avatarColors(
                    for: email,
                    isDark: colorScheme == .dark,
                )
                Text(String(initial))
                    .font(
                        .system(
                            size: diameter * initialFontRatio,
                            weight: .bold
                        )
                    )
                    .foregroundStyle(foreground)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(background)
            } else {
                Image(.filledPerson)
                    .resizable()
                    .scaledToFit()
                    .frame(
                        width: diameter * fallbackIconRatio,
                        height: diameter * fallbackIconRatio,
                    )
                    .foregroundStyle(Color.secondary)
                    .frame(
                        maxWidth: .infinity,
                        maxHeight: .infinity,
                        alignment: .center
                    )
            }
        }
    }
}

private extension String {
    var avatarInitial: Character? {
        let localPart = split(
            separator: "@",
            maxSplits: 1,
            omittingEmptySubsequences: false,
        ).first.map(String.init) ?? self
        return localPart.first { $0.isLetter || $0.isNumber }?
            .uppercased()
            .first
    }

    // Mirrors Java's String.hashCode so a given email maps to the same hue on iOS and Android.
    // Swift's built-in hashValue is randomized per-process, which would make the color drift between launches.
    var javaHashCode: Int32 {
        var h: Int32 = 0
        for codeUnit in utf16 {
            h = h &* 31 &+ Int32(codeUnit)
        }
        return h
    }
}

private func avatarColors(
    for key: String,
    isDark: Bool,
) -> (background: Color, foreground: Color) {
    let normalizedHash = Double(key.javaHashCode & 0x7fffffff)
    let hue = normalizedHash.truncatingRemainder(dividingBy: 360) / 360
    let foreground: RGB = isDark ? .black : .white
    let background = mostColorfulBackground(
        hue: hue,
        saturation: avatarSaturation,
        foreground: foreground,
    )
    return (background.color, foreground.color)
}

// Picks the most vivid background (lightness closest to 0.5) that still meets the
// target contrast ratio against the chosen foreground. Always finds a solution
// because the search bound (pure black for white text, pure white for black text)
// trivially exceeds the target.
private func mostColorfulBackground(
    hue: Double,
    saturation: Double,
    foreground: RGB,
) -> RGB {
    let foregroundIsLight = foreground.luminance > 0.5
    var lo: Double = foregroundIsLight ? 0 : 0.5
    var hi: Double = foregroundIsLight ? 0.5 : 1
    var best: RGB = .hsl(
        hue: hue,
        saturation: saturation,
        lightness: foregroundIsLight ? lo : hi,
    )
    for _ in 0..<contrastSearchSteps {
        let mid = (lo + hi) / 2
        let candidate = RGB.hsl(
            hue: hue,
            saturation: saturation,
            lightness: mid
        )
        let meetsTarget = contrastRatio(
            candidate,
            foreground
        ) >= targetContrastRatio
        if foregroundIsLight {
            if meetsTarget {
                best = candidate
                lo = mid
            } else {
                hi = mid
            }
        } else {
            if meetsTarget {
                best = candidate
                hi = mid
            } else {
                lo = mid
            }
        }
    }
    return best
}

private struct RGB {
    let red: Double
    let green: Double
    let blue: Double

    static let white = RGB(red: 1, green: 1, blue: 1)
    static let black = RGB(red: 0, green: 0, blue: 0)

    var color: Color {
        Color(.sRGB, red: red, green: green, blue: blue, opacity: 1)
    }

    var luminance: Double {
        0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    static func hsl(hue: Double, saturation: Double, lightness: Double) -> RGB {
        let c = (1 - abs(2 * lightness - 1)) * saturation
        let hPrime = (hue * 6).truncatingRemainder(dividingBy: 6)
        let x = c * (1 - abs(hPrime.truncatingRemainder(dividingBy: 2) - 1))
        let m = lightness - c / 2
        let (r, g, b): (Double, Double, Double)
        switch hPrime {
        case 0..<1: (r, g, b) = (c, x, 0)
        case 1..<2: (r, g, b) = (x, c, 0)
        case 2..<3: (r, g, b) = (0, c, x)
        case 3..<4: (r, g, b) = (0, x, c)
        case 4..<5: (r, g, b) = (x, 0, c)
        case 5..<6: (r, g, b) = (c, 0, x)
        default: (r, g, b) = (0, 0, 0)
        }
        return RGB(red: r + m, green: g + m, blue: b + m)
    }
}

private func channel(_ value: Double) -> Double {
    value <= 0.03928 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
}

private func contrastRatio(_ a: RGB, _ b: RGB) -> Double {
    let la = a.luminance
    let lb = b.luminance
    let (bright, dark) = la >= lb ? (la, lb) : (lb, la)
    return (bright + 0.05) / (dark + 0.05)
}

private let initialFontRatio: Double = 0.45
private let fallbackIconRatio: Double = 0.6
private let avatarSaturation: Double = 0.65
private let targetContrastRatio: Double = 7.0
private let contrastSearchSteps = 20

#Preview("Default") {
    EmailAvatar(email: "felipearpa@email.com")
        .frame(width: 64, height: 64)
        .clipShape(Circle())
}

#Preview("Fallback") {
    EmailAvatar(email: "@email.com")
        .frame(width: 64, height: 64)
        .clipShape(Circle())
}
