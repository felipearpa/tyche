import Foundation
import Core
import UI
import Session

public class EmailLinkSignInViewModel: ObservableObject {
    @Published @MainActor private(set) var state: LodableViewState<AccountBundle> = .initial
    
    private let signInWithEmailLinkUseCase: SignInWithEmailLinkUseCase
    
    public init(signInWithEmailLinkUseCase: SignInWithEmailLinkUseCase) {
        self.signInWithEmailLinkUseCase = signInWithEmailLinkUseCase
    }
    
    @MainActor
    func reset() {
        state = .initial
    }
    
    @MainActor
    func signInWithEmailLink(email: String, emailLink: String) {
        Task {
            state = .loading
            
            let result = await signInWithEmailLinkUseCase.execute(email: Email(email)!, emailLink: emailLink)
            switch result {
            case .success(let accountBundle):
                state = .success(accountBundle)
            case .failure(let error):
                state = .failure(error
                    .asEmailLinkSignInLocalizedError()
                    .orLocalizedError())
            }
        }
    }
}
