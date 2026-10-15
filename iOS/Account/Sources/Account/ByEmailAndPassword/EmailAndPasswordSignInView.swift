import SwiftUI
import Session
import UI
import Core
import ViewingState

public struct EmailAndPasswordSignInView: View {
    @StateObject var viewModel: EmailAndPasswordSignInViewModel
    let onSignIn: (AccountBundle) -> Void

    public init(
        viewModel: @autoclosure @escaping () -> EmailAndPasswordSignInViewModel,
        onSignIn: @escaping (AccountBundle) -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onSignIn = onSignIn
    }

    public var body: some View {
        EmailAndPasswordSignInStatefulView(
            viewState: viewModel.state,
            onSignIn: { email, password in
                viewModel.signInWithEmailAndPassword(email: email, password: password)
            },
            onReset: { viewModel.reset() },
            onAuthenticate: onSignIn
        )
        .navigationTitle(String(localized: .signInTitle))
    }
}

/// The sign-in form stays one subtree through signing in, a failure, and a retry, so the entered
/// email and password are kept.
struct EmailAndPasswordSignInStatefulView: View {
    let viewState: LoadState<AccountBundle>
    let onSignIn: (String, String) -> Void
    let onReset: () -> Void
    let onAuthenticate: (AccountBundle) -> Void

    @State private var email: String = ""
    @State private var password: String = ""

    @Environment(\.boxSpacing) private var boxSpacing

    private var isValid: Bool {
        Email.isValid(email) &&
        !password.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private var signIn: (() -> Void)? {
        isValid ? { onSignIn(email, password) } : nil
    }

    var body: some View {
        // The loader stays up after a successful sign-in until the caller routes away, as the
        // form is not usable again once the account is signed in.
        LoadingContainerView(isLoading: viewState.isLoading() || viewState.isLoaded()) {
            SignInContent(
                email: $email,
                password: $password,
                onSignIn: signIn
            )
        }
        .errorAlert(.constant(viewState.errorOrNil()?.localizedErrorOrNil()), onDismiss: onReset)
        // Reported on the change to a signed-in account, so redrawing the screen does not report
        // the same sign-in again.
        .onChange(of: viewState.valueOrNil()) { accountBundle in
            guard let accountBundle else { return }
            onAuthenticate(accountBundle)
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
            RawEmailTextField(value: $email)
            RawPasswordTextField(value: $password)

            Button(action: {
                onSignIn?()
            }) {
                Text(.signInAction)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.standardProminent)
            .disabled(onSignIn == nil)

            Text(.noRecoveryPasswordWarning)
                .font(.caption)
                .foregroundColor(Color(sharedResource: .onSurfaceVariant))
                .padding(.vertical, boxSpacing.small)
                .padding(.horizontal, boxSpacing.small)
                .frame(maxWidth: .infinity)
                .background(
                    Color(sharedResource: .surfaceVariant),
                    in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                )

            Spacer()
        }
        .padding(boxSpacing.medium)
        .textFieldStyle(.standard)
    }
}

#Preview("initial") {
    EmailAndPasswordSignInStatefulView(
        viewState: .idle,
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}

#Preview("loading") {
    EmailAndPasswordSignInStatefulView(
        viewState: .loading,
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}

#Preview("success") {
    EmailAndPasswordSignInStatefulView(
        viewState: .loaded(AccountBundle(accountId: "", externalAccountId: "", email: "")),
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}

#Preview("failure") {
    EmailAndPasswordSignInStatefulView(
        viewState: .failure(UnknownLocalizedError()),
        onSignIn: {_,_ in },
        onReset: {},
        onAuthenticate: { _ in },
    )
}
