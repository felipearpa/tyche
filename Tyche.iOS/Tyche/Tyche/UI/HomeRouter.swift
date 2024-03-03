import SwiftUI
import Swinject
import Core
import Session
import Account
import DataPool

struct HomeRouter: View {
    @State private var signedInAccount: AccountBundle? = nil
    
    var body: some View {
        let _ = Self._printChanges()
        
        if (signedInAccount == nil) {
            HomeContent(
                onAccountSignedIn: { loggedInUser in self.signedInAccount = loggedInUser }
            )
        } else {
            PoolContent(user: signedInAccount!)
        }
    }
}

private struct HomeContent: View {
    let onAccountSignedIn: (AccountBundle) -> Void
    
    @Environment(\.diResolver) var diResolver: DIResolver
    @State private var path = NavigationPath()
    
    var body: some View {
        NavigationStack(path: $path) {
            HomeView(onSignInRequested: { path.append(EmailSignInRoute()) })
                .navigationDestination(for: EmailSignInRoute.self) { loginRoute in
                    EmailSignInView(
                        viewModel: EmailSignInViewModel(
                            sendSignInLinkToEmailUseCase: diResolver.resolve(SendSignInLinkToEmailUseCase.self)!
                        )
                    )
                }
        }
        .task {
            let loginStorage = diResolver.resolve(AccountStorage.self)!
            if let loggedInUser = try? await loginStorage.retrieve() {
                onAccountSignedIn(loggedInUser)
            }
        }
    }
}

struct PoolContent: View {
    let user: AccountBundle
    
    @State private var activePool: PoolProfile? = nil
    
    var body: some View {
        let _ = Self._printChanges()
        
        if activePool == nil {
            PoolScoreListRouter(
                user: user,
                onPoolSelected: { selectedPool in activePool = selectedPool }
            )
        } else {
            PoolHomeRouter(
                user: user,
                pool: activePool!
            )
        }
    }
}
