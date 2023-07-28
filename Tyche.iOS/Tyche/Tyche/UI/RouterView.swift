import SwiftUI
import Swinject
import Core
import User
import Pool

struct RouterView: View {
    @State private var path = NavigationPath()
    @State private var loggedInuser: UserProfile? = nil
    
    var diResolver = DIResolver(
        resolver:Assembler([CoreAssembly(), LoginAssembly(), PoolAssembly()]).resolver
    )
    
    var body: some View {
        let loginStorage = diResolver.resolve(LoginStorage.self)
        
        NavigationStack(path: $path) {
            InitialView(
                diResolver: diResolver,
                user: loggedInuser,
                onLoginRequested: { path.append(LoginRoute()) },
                onPoolDetailRequested: { poolId in path.append(PoolHomeRoute(poolId: poolId)) }
            )
            .onAppear {
                loggedInuser = try! loginStorage?.get()?.user
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
                        loggedInuser = loggedUser
                    }
                )
            }
            .navigationDestination(for: PoolHomeRoute.self) { poolHomeRoute in
                PoolHomeView(
                    diResolver: diResolver,
                    gamblerId: loggedInuser?.userId,
                    poolId: poolHomeRoute.poolId
                )
            }
        }
        .environmentObject(diResolver)
    }
}

private struct InitialView : View {
    let diResolver: DIResolver
    let user: UserProfile?
    let onLoginRequested: () -> Void
    let onPoolDetailRequested: (String) -> Void
    
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
                ),
                onPoolDetailRequested: onPoolDetailRequested
            )
        }
    }
}

struct RouterView_Previews: PreviewProvider {
    static var previews: some View {
        RouterView()
    }
}
