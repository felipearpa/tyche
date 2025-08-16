import SwiftUI

public struct PaginVStackRetryableError: View {
    private let localizedError: LocalizedError
    private let retryAction: () -> Void
    
    public init(localizedError: LocalizedError, retryAction: @escaping () -> Void) {
        self.localizedError = localizedError
        self.retryAction = retryAction
    }
    
    public var body: some View {
        VStack(spacing: 8) {
            ErrorView(localizedError: localizedError)
            
            Button(action: {
            }) {
                Text(String(.retryAction))
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

struct PaginVStackRetryableError_Previews: PreviewProvider {
    static var previews: some View {
        PaginVStackRetryableError(localizedError: UnknownLocalizedError(), retryAction: {})
    }
}
