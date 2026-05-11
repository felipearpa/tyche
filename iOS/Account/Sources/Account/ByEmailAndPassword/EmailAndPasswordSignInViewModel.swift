import Foundation
import Core
import UI
import Session
import ViewingState

public class EmailAndPasswordSignInViewModel: ObservableObject {
    @Published @MainActor private(set) var state: LoadState<AccountBundle> = .idle

    private let signInWithEmailAndPasswordUseCase: SignInWithEmailAndPasswordUseCase

    public init(signInWithEmailAndPasswordUseCase: SignInWithEmailAndPasswordUseCase) {
        self.signInWithEmailAndPasswordUseCase = signInWithEmailAndPasswordUseCase
    }
    
    @MainActor
    func reset() {
        state = .idle
    }
    
    @MainActor
    func signInWithEmailAndPassword(email: String, password: String) {
        Task {
            state = .loading
            
            let result = await signInWithEmailAndPasswordUseCase.execute(email: Email(email)!, password: password)
            switch result {
            case .success(let accountBundle):
                state = .loaded(accountBundle)
            case .failure(let error):
                state = .failure(error.mapOrDefaultLocalized { $0.asEmailAndPasswordSignInLocalizedError() })
            }
        }
    }
}
