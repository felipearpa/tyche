import Foundation
import UIKit
import GoogleSignIn
import Core
import UI
import Session
import ViewingState

public class GoogleSignInViewModel: ObservableObject {
    @Published @MainActor public private(set) var state: LoadState<AccountBundle> = .idle

    private let signInWithGoogleUseCase: SignInWithGoogleUseCase

    public init(signInWithGoogleUseCase: SignInWithGoogleUseCase) {
        self.signInWithGoogleUseCase = signInWithGoogleUseCase
    }

    @MainActor
    public func reset() {
        state = .idle
    }

    @MainActor
    public func signInWithGoogle(presenting presentingViewController: UIViewController) {
        Task {
            let gidResult: GIDSignInResult
            do {
                gidResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController)
            } catch let error as NSError where error.code == GIDSignInError.canceled.rawValue {
                return
            } catch {
                state = .failure(error.mapOrDefaultLocalized { $0.asGoogleSignInLocalizedError() })
                return
            }

            guard let idToken = gidResult.user.idToken?.tokenString else {
                state = .failure(GoogleSignInLocalizedError.invalidCredential)
                return
            }
            let accessToken = gidResult.user.accessToken.tokenString

            state = .loading

            let result = await signInWithGoogleUseCase.execute(idToken: idToken, accessToken: accessToken)
            switch result {
            case .success(let accountBundle):
                state = .loaded(accountBundle)
            case .failure(let error):
                state = .failure(error.mapOrDefaultLocalized { $0.asGoogleSignInLocalizedError() })
            }
        }
    }
}
