import SwiftUI
import UIKit
import Session
import Core
import UI
import Account

struct HomeRouter: View {
    let onSignIn: (AccountBundle) -> Void

    @State private var path = NavigationPath()
    @StateObject private var googleSignInViewModel: GoogleSignInViewModel
    @Environment(\.diResolver) var diResolver: DIResolver

    init(onSignIn: @escaping (AccountBundle) -> Void, diResolver: DIResolver) {
        self.onSignIn = onSignIn
        self._googleSignInViewModel = StateObject(
            wrappedValue: GoogleSignInViewModel(
                signInWithGoogleUseCase: diResolver.resolve(SignInWithGoogleUseCase.self)!
            )
        )
    }

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onSignInWithEmail: { path.append(EmailSignInRoute()) },
                onSignInWithEmailAndPassword: { path.append(EmailAndPasswordSignInRoute()) },
                socialSignInSlot: {
                    SocialSignInRow(
                        googleState: googleSignInViewModel.state,
                        onSignInWithGoogle: { googleSignInViewModel.signInWithGoogle(presenting: topViewController()) },
                    )
                },
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
                    onSignIn: onSignIn,
                )
            }
            .overlay {
                googleOverlay
            }
            .errorAlert(
                .constant(googleSignInViewModel.state.errorOrNil()?.localizedErrorOrNil()),
                onDismiss: { googleSignInViewModel.reset() }
            )
        }
    }

    @ViewBuilder
    private var googleOverlay: some View {
        switch googleSignInViewModel.state {
        case .loading:
            LoadingContainerView { Color.clear }
        case .loaded(let accountBundle):
            Color.clear
                .onAppear { onSignIn(accountBundle) }
        case .idle, .failure:
            EmptyView()
        }
    }

    private func topViewController() -> UIViewController {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: { $0.isKeyWindow }) ?? UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first
        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top ?? UIViewController()
    }
}
