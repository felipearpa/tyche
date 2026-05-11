import Foundation
import Core
import UI
import Session
import ViewingState

public class EmailLinkSignInViewModel: ObservableObject {
    @Published @MainActor private(set) var state: LoadState<AccountBundle> = .idle

    private let signInWithEmailLinkUseCase: SignInWithEmailLinkUseCase
    
    public init(signInWithEmailLinkUseCase: SignInWithEmailLinkUseCase) {
        self.signInWithEmailLinkUseCase = signInWithEmailLinkUseCase
    }
    
    @MainActor
    func reset() {
        state = .idle
    }
    
    @MainActor
    func signInWithEmailLink(email: String, emailLink: String) {
        Task {
            state = .loading

            let result = await signInWithEmailLinkUseCase.execute(email: Email(email)!, emailLink: emailLink)
            switch result {
            case .success(let accountBundle):
                state = .loaded(accountBundle)
            case .failure(let error):
                state = .failure(error.mapOrDefaultLocalized { $0.asEmailLinkSignInLocalizedError() })
            }
        }
    }
}
