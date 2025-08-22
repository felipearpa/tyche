import SwiftUI

public struct StatefulLazyVStackEmpty: View {
    public init() {}
    
    public var body: some View {
        MessageView(
            icon: Image(.search),
            message: String(.emptyListMessage)
        )
    }
}

#Preview {
    StatefulLazyVStackEmpty()
}
