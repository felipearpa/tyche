import SwiftUI

/// Deterministic initial rendering shared by email-backed and username-backed avatars.
public struct InitialAvatar: View {
    private let identity: String
    private let colorKey: String
    private let backgroundColor: Color?
    private let foregroundColor: Color?

    @Environment(\.colorScheme) private var colorScheme

    public init(
        identity: String,
        colorKey: String? = nil,
        backgroundColor: Color? = nil,
        foregroundColor: Color? = nil
    ) {
        self.identity = identity
        self.colorKey = colorKey ?? identity
        self.backgroundColor = backgroundColor
        self.foregroundColor = foregroundColor
    }

    public var body: some View {
        GeometryReader { proxy in
            let diameter = min(proxy.size.width, proxy.size.height)

            if let initial = identity.avatarInitial {
                let generatedColors = avatarColors(
                    for: colorKey,
                    isDark: colorScheme == .dark
                )
                Text(String(initial))
                    .font(
                        .system(
                            size: diameter * initialFontRatio,
                            weight: .bold
                        )
                    )
                    .foregroundStyle(foregroundColor ?? generatedColors.foreground)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(backgroundColor ?? generatedColors.background)
            } else {
                Image(.filledPerson)
                    .resizable()
                    .scaledToFit()
                    .frame(
                        width: diameter * fallbackIconRatio,
                        height: diameter * fallbackIconRatio
                    )
                    .foregroundStyle(foregroundColor ?? Color.secondary)
                    .frame(
                        maxWidth: .infinity,
                        maxHeight: .infinity,
                        alignment: .center
                    )
                    .background(backgroundColor ?? Color.clear)
            }
        }
    }
}

extension String {
    var avatarInitial: Character? {
        first { $0.isLetter || $0.isNumber }?
            .uppercased()
            .first
    }

    var emailAvatarIdentity: String {
        split(
            separator: "@",
            maxSplits: 1,
            omittingEmptySubsequences: false
        ).first.map(String.init) ?? self
    }

    var javaHashCode: Int32 {
        var hash: Int32 = 0
        for codeUnit in utf16 {
            hash = hash &* 31 &+ Int32(codeUnit)
        }
        return hash
    }
}

func avatarColors(
    for key: String,
    isDark: Bool
) -> (background: Color, foreground: Color) {
    let colors = avatarRGBColors(for: key, isDark: isDark)
    return (colors.background.color, colors.foreground.color)
}

func avatarRGBColors(
    for key: String,
    isDark: Bool
) -> (background: AvatarRGB, foreground: AvatarRGB) {
    let normalizedHash = Double(key.javaHashCode & 0x7fffffff)
    let hue = normalizedHash.truncatingRemainder(dividingBy: 360) / 360
    let foreground: AvatarRGB = isDark ? .black : .white
    let background = mostColorfulBackground(
        hue: hue,
        saturation: avatarSaturation,
        foreground: foreground
    )
    return (background, foreground)
}

private func mostColorfulBackground(
    hue: Double,
    saturation: Double,
    foreground: AvatarRGB
) -> AvatarRGB {
    let foregroundIsLight = foreground.luminance > 0.5
    var lowerBound: Double = foregroundIsLight ? 0 : 0.5
    var upperBound: Double = foregroundIsLight ? 0.5 : 1
    var best = AvatarRGB.hsl(
        hue: hue,
        saturation: saturation,
        lightness: foregroundIsLight ? lowerBound : upperBound
    )

    for _ in 0..<contrastSearchSteps {
        let midpoint = (lowerBound + upperBound) / 2
        let candidate = AvatarRGB.hsl(
            hue: hue,
            saturation: saturation,
            lightness: midpoint
        )
        let meetsTarget = contrastRatio(candidate, foreground) >= targetContrastRatio

        if foregroundIsLight {
            if meetsTarget {
                best = candidate
                lowerBound = midpoint
            } else {
                upperBound = midpoint
            }
        } else if meetsTarget {
            best = candidate
            upperBound = midpoint
        } else {
            lowerBound = midpoint
        }
    }

    return best
}

struct AvatarRGB {
    let red: Double
    let green: Double
    let blue: Double

    static let white = AvatarRGB(red: 1, green: 1, blue: 1)
    static let black = AvatarRGB(red: 0, green: 0, blue: 0)

    var color: Color {
        Color(.sRGB, red: red, green: green, blue: blue, opacity: 1)
    }

    var luminance: Double {
        0.2126 * linearChannel(red)
            + 0.7152 * linearChannel(green)
            + 0.0722 * linearChannel(blue)
    }

    static func hsl(
        hue: Double,
        saturation: Double,
        lightness: Double
    ) -> AvatarRGB {
        let chroma = (1 - abs(2 * lightness - 1)) * saturation
        let hueSegment = (hue * 6).truncatingRemainder(dividingBy: 6)
        let secondary = chroma
            * (1 - abs(hueSegment.truncatingRemainder(dividingBy: 2) - 1))
        let match = lightness - chroma / 2
        let components: (Double, Double, Double)

        switch hueSegment {
        case 0..<1: components = (chroma, secondary, 0)
        case 1..<2: components = (secondary, chroma, 0)
        case 2..<3: components = (0, chroma, secondary)
        case 3..<4: components = (0, secondary, chroma)
        case 4..<5: components = (secondary, 0, chroma)
        case 5..<6: components = (chroma, 0, secondary)
        default: components = (0, 0, 0)
        }

        return AvatarRGB(
            red: components.0 + match,
            green: components.1 + match,
            blue: components.2 + match
        )
    }
}

func contrastRatio(_ first: AvatarRGB, _ second: AvatarRGB) -> Double {
    let brighter = max(first.luminance, second.luminance)
    let darker = min(first.luminance, second.luminance)
    return (brighter + 0.05) / (darker + 0.05)
}

private func linearChannel(_ value: Double) -> Double {
    value <= 0.03928
        ? value / 12.92
        : pow((value + 0.055) / 1.055, 2.4)
}

private let initialFontRatio: Double = 0.45
private let fallbackIconRatio: Double = 0.6
private let avatarSaturation: Double = 0.65
private let targetContrastRatio: Double = 7.0
private let contrastSearchSteps = 20

#Preview("Username") {
    InitialAvatar(identity: "ElGoleador")
        .frame(width: 64, height: 64)
        .clipShape(Circle())
}

#Preview("Fallback") {
    InitialAvatar(identity: "___")
        .frame(width: 64, height: 64)
        .clipShape(Circle())
}
