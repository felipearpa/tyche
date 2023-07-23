import SwiftUI

public struct ErrorView: View {
    private let localizedError: LocalizedError
    
    public init(localizedError: LocalizedError) {
        self.localizedError = localizedError
    }
    
    public var body: some View {
        VStack(spacing: 8) {
            ResourceScheme.sentimentDissatisfied.image
                .resizable()
                .frame(width: 40, height: 40)
            
            if let errorDescription = localizedError.errorDescription {
                Text(errorDescription)
                    .font(.title)
            }
            
            if let failureReason = localizedError.failureReason {
                Text(failureReason)
            }
        }
    }
}

struct ErrorView_Previews: PreviewProvider {
    static var previews: some View {
        ErrorView(localizedError: UnknownLocalizedError())
    }
}
