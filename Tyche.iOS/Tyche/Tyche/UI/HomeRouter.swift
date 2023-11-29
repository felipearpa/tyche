import SwiftUI
import Swinject
import Core
import Session
import Account
import DataPool

struct HomeRouter: View {
    let diResolver: DIResolver
    
    @State private var loggedInUser: AccountBundle? = nil
    
    var body: some View {
        let _ = Self._printChanges()
        
        if (loggedInUser == nil) {
            HomeContent(
                diResolver: diResolver,
                onUserLoggedIn: { loggedInUser in self.loggedInUser = loggedInUser }
            )
        } else {
            PoolContent(diResolver: diResolver, user: loggedInUser!)
        }
    }
}

private struct HomeContent: View {
    let diResolver: DIResolver
    let onUserLoggedIn: (AccountBundle) -> Void
    
    @State private var path = NavigationPath()
    
    var body: some View {
        NavigationStack(path: $path) {
            HomeView(onLoginRequested: { path.append(LoginRoute()) })
                .navigationDestination(for: LoginRoute.self) { loginRoute in
                    LoginView(
                        viewModel: LoginViewModel(
                            loginUseCase: LoginUseCase(
                                loginRepository: diResolver.resolve(LoginRepository.self)!,
                                authStorage: diResolver.resolve(AuthStorage.self)!,
                                accountStorage: diResolver.resolve(AccountStorage.self)!
                            )
                        ),
                        onLogin: { loggedInUser in onUserLoggedIn(loggedInUser) }
                    )
                }
        }
        .task {
            let loginStorage = diResolver.resolve(AccountStorage.self)!
            if let loggedInUser = try? await loginStorage.retrieve() {
                onUserLoggedIn(loggedInUser)
            }
        }
    }
}

private struct PoolContent: View {
    let diResolver: DIResolver
    let user: AccountBundle
    
    @State private var activePool: PoolProfile? = nil
    
    var body: some View {
        let _ = Self._printChanges()

        if activePool == nil {
            PoolScoreListRouter(
                diResolver: diResolver,
                user: user,
                onPoolSelected: { selectedPool in activePool = selectedPool }
            )
        } else {
            PoolHomeRouter(
                diResolver: diResolver,
                user: user,
                pool: activePool!
            )
        }
    }
}

#Preview {
    HomeRouter(diResolver: DIResolver(resolver:Assembler([]).resolver))
}
