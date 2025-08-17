import SwiftUI
import Session
import UI
import Core

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
        .navigationTitle(String(.signInTitle))
    }
}

private struct EmailAndPasswordSignInStatefulView: View {
    let viewState: LoadableViewState<AccountBundle>
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
        switch viewState {
        case .initial:
            SignInContent(
                email: $email,
                password: $password,
                onSignIn: signIn
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
            RawEmailTextField(value: $email)
            RawPasswordTextField(value: $password)

            Button(action: {
                onSignIn?()
            }) {
                Text(String(.signInAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(onSignIn == nil)

            Text(String(.noRecoveryPasswordWarning))
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
        .textFieldStyle(.roundedBorder)
    }
}

#Preview("initial") {
    EmailAndPasswordSignInStatefulView(
        viewState: .initial,
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
        viewState: .success(AccountBundle(accountId: "", externalAccountId: "")),
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
