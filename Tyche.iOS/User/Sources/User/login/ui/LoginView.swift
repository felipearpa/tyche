import SwiftUI
import Core
import UI

public struct LoginView: View {
    @ObservedObject private var viewModel: LoginViewModel
    @State private var loginCrendential = LoginCredentialModel(username: "", password: "")
    @State private var isValid = false
    
    public init(viewModel: LoginViewModel) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        let _ = print("viewState: \(viewModel.state)")
        
        ZStack {
            VStack {
                UsernameTextField(value: $loginCrendential.username)
                PasswordTextField(value: $loginCrendential.password)
            }
            .frame(maxHeight: .infinity, alignment: .top)
            .textFieldStyle(.roundedBorder)
            .padding()
            .navigationTitle(StringScheme.loginTitle.localizedKey)
            .toolbar {
                Button(StringScheme.loginAction.localizedKey) {
                    viewModel.login(loginCredential: loginCrendential)
                }
                .disabled(!isValid || viewModel.state.isLoading())
            }
            .blur(radius: viewModel.state.isLoading() ? 3 : 0)
            .errorAlert(.constant(viewModel.state.localizedErrorWrapperOrNull())) {
                viewModel.resetState()
            }
            .onChange(of: loginCrendential) { newLoginCredential in
                isValid = newLoginCredential.isValid()
            }
            
            if viewModel.state.isLoading() {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.white.opacity(0.5))
            }
        }
    }
}

private class FakeLoginRepository : LoginRepository {
    func login(loginCredential: LoginCredential) async -> Result<LoginProfile, Error> {
        return .success(LoginProfile(
            token: "",
            user: UserProfile(userId: "", username: "")
        ))
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView(
            viewModel: LoginViewModel(
                loginUseCase: LoginUseCase(
                    loginRepository: FakeLoginRepository()
                )
            )
        )
    }
}
