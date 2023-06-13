import SwiftUI
import Swinject
import Core
import User

struct RouterView: View {
    var diResolver = DIResolver(
        resolver:Assembler([CoreAssembly(), LoginAssembly()]).resolver
    )
    
    var body: some View {
        NavigationStack {
            HomeView()
                .navigationDestination(for: LoginRoute.self) { loginRoute in
                    LoginView(
                        viewModel: LoginViewModel(
                            loginUseCase: LoginUseCase(
                                loginRepository: diResolver.resolve(LoginRepository.self)!
                            )
                        )
                    )
                }
        }.environmentObject(diResolver)
    }
}

struct RouterView_Previews: PreviewProvider {
    static var previews: some View {
        RouterView()
    }
}
