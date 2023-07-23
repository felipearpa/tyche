import SwiftUI
import Swinject
import Core
import User
import Pool

struct RouterView: View {
    @State private var path = NavigationPath()
    @State private var user: UserProfile? = nil
    
    var diResolver = DIResolver(
        resolver:Assembler([CoreAssembly(), LoginAssembly(), PoolAssembly()]).resolver
    )
    
    var body: some View {
        let loginStorage = diResolver.resolve(LoginStorage.self)
        
        NavigationStack(path: $path) {
            InitialView(
                user: user,
                onLoginRequested: { path.append(LoginRoute()) }
            )
            .onAppear {
                user = try! loginStorage?.get()?.user
            }
            .navigationDestination(for: LoginRoute.self) { loginRoute in
                LoginView(
                    viewModel: LoginViewModel(
                        loginUseCase: LoginUseCase(
                            loginRepository: diResolver.resolve(LoginRepository.self)!,
                            loginStorage: diResolver.resolve(LoginStorage.self)!
                        )
                    ),
                    onLogin: { loggedUser in
                        path.removeLast(path.count)
                        user = loggedUser
                    }
                )
            }
        }
        .environmentObject(diResolver)
    }
}

private struct InitialView : View {
    let user: UserProfile?
    let onLoginRequested: () -> Void
    @EnvironmentObject var diResolver: DIResolver
    
    var body: some View {
        if user == nil {
            HomeView(onLoginRequested: onLoginRequested)
        } else {
            PoolScoreListView(
                viewModel: PoolScoreListViewModel(
                    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    gamblerId: user!.userId
                )
            )
        }
    }
}

struct RouterView_Previews: PreviewProvider {
    static var previews: some View {
        RouterView()
    }
}
