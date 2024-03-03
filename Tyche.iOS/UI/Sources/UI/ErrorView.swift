import SwiftUI

public struct ErrorView: View {
    private let localizedError: LocalizedError
    
    @Environment(\.boxSpacing) private var boxSpacing
    
    public init(localizedError: LocalizedError) {
        self.localizedError = localizedError
    }
    
    public var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(.sentimentDissatisfied)
                .resizable()
                .frame(width: 40, height: 40)
                .foregroundStyle(Color(.error))
            
            VStack(spacing: boxSpacing.medium) {
                Text(localizedError.errorDescription ?? "")
                    .font(.title)
                
                Text([localizedError.failureReason, localizedError.recoverySuggestion]
                    .compactMap { string in string }.joined(separator: ". ")
                )
                .multilineTextAlignment(.center)
            }
        }
    }
}

struct ErrorView_Previews: PreviewProvider {
    static var previews: some View {
        ErrorView(localizedError: UnknownLocalizedError())
    }
}
