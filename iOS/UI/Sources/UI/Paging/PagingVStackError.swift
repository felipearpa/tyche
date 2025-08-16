import SwiftUI

public struct PagingVStackError: View {
    let localizedError: LocalizedError
    
    public init(localizedError: LocalizedError) {
        self.localizedError = localizedError
    }
    
    public var body: some View {
        ErrorView(localizedError: localizedError)
    }
}

struct PagingVStackError_Previews: PreviewProvider {
    static var previews: some View {
        PagingVStackError(localizedError: UnknownLocalizedError())
    }
}
