import SwiftUI
import Swinject
import Core
import Session
import Account
import DataPool

struct HomeRouter: View {
    @State private var signedInAccountBundle: AccountBundle? = nil
    @Environment(\.diResolver) var diResolver: DIResolver

    var body: some View {
        let _ = Self._printChanges()

        Group {
            if (signedInAccountBundle == nil) {
                HomeContent(
                    onSignIn: { loggedInUser in signedInAccountBundle = loggedInUser }
                )
            } else {
                PoolContent(
                    user: signedInAccountBundle!,
                    onLogout: { signedInAccountBundle = nil }
                )
            }
        }
        .task {
            let accountStorage = diResolver.resolve(AccountStorage.self)!
            if let accountBundle = try? await accountStorage.retrieve() {
                signedInAccountBundle = accountBundle
            }
        }
    }
}

private struct HomeContent: View {
    let onSignIn: (AccountBundle) -> Void

    @Environment(\.diResolver) var diResolver: DIResolver
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onSignInWithEmail: { path.append(EmailSignInRoute()) },
                onSignInWithEmailAndPassword: { path.append(EmailAndPasswordSignInRoute()) },
            )
            .navigationDestination(for: EmailSignInRoute.self) { route in
                EmailSignInView(
                    viewModel: EmailSignInViewModel(
                        sendSignInLinkToEmailUseCase: diResolver.resolve(SendSignInLinkToEmailUseCase.self)!
                    )
                )
            }
            .navigationDestination(for: EmailAndPasswordSignInRoute.self) { route in
                EmailAndPasswordSignInView(
                    viewModel: EmailAndPasswordSignInViewModel(
                        signInWithEmailAndPasswordUseCase: diResolver.resolve(SignInWithEmailAndPasswordUseCase.self)!
                    ),
                    onAuthenticate: { accountBundle in
                        onSignIn(accountBundle)
                    },
                )
            }
        }
    }
}

struct PoolContent: View {
    let user: AccountBundle
    let onLogout: () -> Void

    @State private var activePool: PoolProfile? = nil

    var body: some View {
        let _ = Self._printChanges()

        if activePool == nil {
            PoolScoreListRouter(
                user: user,
                onPoolSelected: { selectedPool in activePool = selectedPool },
                onLogout: onLogout,
            )
        } else {
            PoolHomeRouter(
                user: user,
                pool: activePool!
            )
        }
    }
}
