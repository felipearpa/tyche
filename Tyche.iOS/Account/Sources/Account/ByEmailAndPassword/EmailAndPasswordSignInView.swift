import SwiftUI
import Session
import UI
import Core

public struct EmailAndPasswordSignInView: View {
    @StateObject var viewModel: EmailAndPasswordSignInViewModel
    let onAuthenticate: (AccountBundle) -> Void

    public init(
        viewModel: @autoclosure @escaping () -> EmailAndPasswordSignInViewModel,
        onAuthenticate: @escaping (AccountBundle) -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onAuthenticate = onAuthenticate
    }

    public var body: some View {
        EmailAndPasswordSignInStateView(
            viewState: viewModel.state,
            onSignIn: { email, password in
                viewModel.signInWithEmailAndPassword(email: email, password: password)
            },
            onReset: { viewModel.reset() },
            onAuthenticate: onAuthenticate
        )
    }
}

private struct EmailAndPasswordSignInStateView: View {
    let viewState: LoadableViewState<AccountBundle>
    let onSignIn: (String, String) -> Void
    let onReset: () -> Void
    let onAuthenticate: (AccountBundle) -> Void

    @State private var email: String = ""
    @State private var password: String = ""

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        switch viewState {
        case .initial:
            SignInContent(
                email: $email,
                password: $password,
                onSignIn: {
                    if Email.isValid(email) {
                        onSignIn(email, password)
                    }
                },
            )
        case .loading:
            LoadingContainerView {
                SignInContent(
                    email: $email,
                    password: $password,
                )
            }
        case .success(let accountBundle):
            LoadingContainerView {
                SignInContent(
                    email: $email,
                    password: $password,
                )
            }
            .onAppear {
                onAuthenticate(accountBundle)
            }
        case .failure(let error):
            SignInContent(
                email: $email,
                password: $password,
            )
            .errorAlert(.constant(error.localizedErrorOrNil()!), onDismiss: onReset)
        }
    }
}

private struct SignInContent: View {
    @Binding var email: String
    @Binding var password: String
    var onSignIn: (() -> Void)? = nil

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            EmailTextField(value: $email)

            SecureField("Password", text: $password)

            Button(action: {
                onSignIn?()
            }) {
                Text("Sign In")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(onSignIn == nil)

            Spacer()
        }
        .padding(boxSpacing.medium)
        .textFieldStyle(.roundedBorder)
    }
}

#Preview("initial") {
    EmailAndPasswordSignInStateView(
        viewState: .initial,
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}

#Preview("loading") {
    EmailAndPasswordSignInStateView(
        viewState: .loading,
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}

#Preview("success") {
    EmailAndPasswordSignInStateView(
        viewState: .success(AccountBundle(accountId: "", externalAccountId: "")),
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}

#Preview("failure") {
    EmailAndPasswordSignInStateView(
        viewState: .failure(UnknownLocalizedError()),
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}
