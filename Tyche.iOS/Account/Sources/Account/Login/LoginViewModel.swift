import Foundation
import Core
import UI
import Session

public class LoginViewModel: ObservableObject {
    @Published @MainActor private(set) var state: LodableViewState<AccountBundle> = .initial
    
    private let loginUseCase: LoginUseCase
    
    public init(loginUseCase: LoginUseCase) {
        self.loginUseCase = loginUseCase
    }
    
    func resetState() {
        Task {
            await MainActor.run {
                state = .initial
            }
        }
    }
    
    func login(loginCredential: LoginCredentialModel) {
        Task {
            await MainActor.run {
                state = .loading
            }
            
            let result = await loginUseCase.execute(loginCredential: loginCredential.toLoginCredential())
            switch result {
            case .success(let userProfile):
                await MainActor.run {
                    state = .success(userProfile)
                }
            case .failure(let error):
                await MainActor.run {
                    state = .failure(error.toLocalizedError())
                }
            }
        }
    }
}

private extension Error {
    func toLocalizedError() -> Error {
        if let loginError = self as? LoginError {
            return loginError.toLoginLocalizedError()
        } else if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }
        
        return UnknownLocalizedError()
    }
}