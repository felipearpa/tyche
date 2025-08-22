import SwiftUI

public struct StatefulLazyVStackError: View {
    let localizedError: LocalizedError

    public init(localizedError: LocalizedError) {
        self.localizedError = localizedError
    }

    public var body: some View {
        ErrorView(localizedError: localizedError)
    }
}

#Preview {
    StatefulLazyVStackError(localizedError: UnknownLocalizedError())
}
