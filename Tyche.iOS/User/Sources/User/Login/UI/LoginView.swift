import SwiftUI
import Core
import UI

public struct LoginView: View {
    @ObservedObject private var viewModel: LoginViewModel
    private let onLogin: (UserProfile) -> Void
    
    @State private var loginCredential = LoginCredentialModel(username: "", password: "")
    @State private var isValid = false
    
    public init(viewModel: LoginViewModel, onLogin: @escaping (UserProfile) -> Void) {
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
        .navigationTitle(StringScheme.loginTitle.localizedKey)
        .navigationBarBackButtonHidden(viewModel.state.isLoading())
    }
}

private struct MainContent: View {
    let onLogin: (LoginCredentialModel) -> Void
    let reset: () -> Void
    let viewState: ViewState<UserProfile>

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
            Button(StringScheme.loginAction.localizedKey) {
                onLogin(loginCredential)
            }
            .disabled(!isValid || viewState.isLoading())
        }
        .blur(radius: viewState.isLoading() ? 3 : 0)
        .errorAlert(.constant(viewState.localizedErrorWrapperOrNull())) {
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
                icon: ResourceScheme.sentimentVerySatisfied.image,
                message: StringScheme.successLoginMessage.localizedString
            )
            
            Button(action: { onContinue() }) {
                Text(StringScheme.continueAction.localizedKey)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

struct LoginView_Previews: PreviewProvider {
    private class FakeLoginRepository : LoginRepository {
        func login(loginCredential: LoginCredential) async -> Result<LoginProfile, Error> {
            return .success(LoginProfile(
                token: "",
                user: UserProfile(userId: "", username: "")
            ))
        }
    }
    
    private class FakeLoginStorage : LoginStorage {
        func store(loginProfile: LoginProfile) throws {
        }
        
        func get() throws -> LoginProfile? {
            return nil
        }
    }
    
    static var previews: some View {
        LoginView(
            viewModel: LoginViewModel(
                loginUseCase: LoginUseCase(
                    loginRepository: FakeLoginRepository(),
                    loginStorage: FakeLoginStorage()
                )
            ),
            onLogin: { _ in }
        )
    }
}
