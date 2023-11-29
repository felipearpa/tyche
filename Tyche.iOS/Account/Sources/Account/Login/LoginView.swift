import SwiftUI
import Core
import UI
import Session

public struct LoginView: View {
    @ObservedObject private var viewModel: LoginViewModel
    private let onLogin: (AccountBundle) -> Void
    
    @State private var loginCredential = LoginCredentialModel(username: "", password: "")
    @State private var isValid = false
    
    public init(
        viewModel: LoginViewModel,
        onLogin: @escaping (AccountBundle) -> Void
    ) {
        self.viewModel = viewModel
        self.onLogin = onLogin
    }
    
    public var body: some View {
        ZStack {
            if !viewModel.state.isSuccess() {
                MainContent(
                    onLogin: viewModel.login,
                    reset: viewModel.resetState,
                    viewState: viewModel.state
                )
            }
            
            if viewModel.state.isLoading() {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.white.opacity(0.5))
            } else if case .success(let loggedInUser) = viewModel.state {
                SuccessContent(onContinue: { onLogin(loggedInUser) })
                    .padding(8)
            }
        }
        .navigationTitle(String(.loginTitle))
        .navigationBarBackButtonHidden(viewModel.state.isLoading())
    }
}

private struct MainContent: View {
    let onLogin: (LoginCredentialModel) -> Void
    let reset: () -> Void
    let viewState: LodableViewState<AccountBundle>

    @State private var loginCredential = LoginCredentialModel(username: "", password: "")
    @State private var isValid = false
    
    var body: some View {
        VStack {
            UsernameTextField(value: $loginCredential.username)
            PasswordTextField(value: $loginCredential.password)
            
            Spacer()
        }
        .textFieldStyle(.roundedBorder)
        .padding(8)
        .toolbar {
            Button(String(.loginAction)) {
                onLogin(loginCredential)
            }
            .disabled(!isValid || viewState.isLoading())
        }
        .blur(radius: viewState.isLoading() ? 3 : 0)
        .errorAlert(.constant(viewState.localizedErrorOrNull())) {
            reset()
        }
        .onChange(of: loginCredential) { newLoginCredential in
            isValid = newLoginCredential.isValid()
        }
    }
}

private struct SuccessContent: View {
    let onContinue: () -> Void
    
    var body: some View {
        VStack(spacing: 8) {
            MessageView(
                icon: Image(sharedResource: .sentimentVerySatisfied),
                message: String(.successLoginMessage)
            )
            
            Button(action: { onContinue() }) {
                Text((String(.continueAction)))
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

#Preview {
    LoginView(
        viewModel: LoginViewModel(
            loginUseCase: LoginUseCase(
                loginRepository: LoginFakeRepository(),
                authStorage: AuthFakeStorage(),
                accountStorage: AccountFakeStorage()
            )
        ),
        onLogin: { _ in }
    )
}
