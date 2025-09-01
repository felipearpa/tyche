import SwiftUI

public struct LoadingContainerView<Content: View>: View {
    private let content: () -> Content
    
    public init(content: @escaping () -> Content) {
        self.content = content
    }
    
    public var body: some View {
        ZStack {
            content()
                .blur(radius: BLUR_RADIUS)
            
            BallSpinner()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
        }
    }
}

private let BLUR_RADIUS: CGFloat = 2
private let ICON_SIZE: CGFloat = 64

#Preview {
    LoadingContainerView {
        Text("Content")
    }
}
