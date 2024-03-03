import SwiftUI

private let BLUR_RADIUS: CGFloat = 2

public struct ProgressContainerView<Content: View>: View {
    private let content: () -> Content
    
    public init(content: @escaping () -> Content) {
        self.content = content
    }
    
    public var body: some View {
        ZStack {
            content()
                .blur(radius: BLUR_RADIUS)
            
            BallSpinner()
        }
    }
}

#Preview {
    ProgressContainerView {
        Text("Content")
    }
}
