import SwiftUI

public struct PagingVStackEmpty: View {
    public init() {}
    
    public var body: some View {
        MessageView(
            icon: ResourceScheme.search.image,
            message: StringScheme.emptyListMessage.localizedString
        )
    }
}

struct PagingVStackEmpty_Previews: PreviewProvider {
    static var previews: some View {
        PagingVStackEmpty()
    }
}
