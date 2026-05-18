import SwiftUI
import UI

extension View {
    func scoreWidth() -> some View {
        let textWidth = String(repeating: "0", count: 3)
            .widthOfString(usingFont: UIFont.preferredFont(from: .body))
        let horizontalPadding = LiquidGlassTextFieldStyle.contentPadding * 2
        return self.frame(width: textWidth + horizontalPadding)
    }
}
