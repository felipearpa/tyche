import Foundation
import Core
import UI
import Session

public class EmailSignInViewModel: ObservableObject {
    @Published @MainActor private(set) var state: LoadableViewState<Void> = .initial
    
    private let sendSignInLinkToEmailUseCase: SendSignInLinkToEmailUseCase
    
    public init(sendSignInLinkToEmailUseCase: SendSignInLinkToEmailUseCase) {
        self.sendSignInLinkToEmailUseCase = sendSignInLinkToEmailUseCase
    }
    
    @MainActor
    func reset() {
        state = .initial
    }
    
    @MainActor
    func sendSignInLinkToEmail(email: String) {
        Task {
            state = .loading
            
            let result = await sendSignInLinkToEmailUseCase.execute(email: Email(email)!)
            switch result {
            case .success:
                state = .success(())
            case .failure(let error):
                state = .failure(error.mapOrDefaultLocalized { $0.asSendSignInLinkToEmailLocalizedError() })
            }
        }
    }
}
