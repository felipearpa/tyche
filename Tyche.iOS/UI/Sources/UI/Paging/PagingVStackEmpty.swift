import SwiftUI

public struct PagingVStackEmpty: View {
    public init() {}
    
    public var body: some View {
        MessageView(
            icon: Image(.search),
            message: String(.emptyListMessage)
        )
    }
}

struct PagingVStackEmpty_Previews: PreviewProvider {
    static var previews: some View {
        PagingVStackEmpty()
    }
}
