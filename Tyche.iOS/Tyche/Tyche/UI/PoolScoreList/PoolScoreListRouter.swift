import SwiftUI
import UI
import Swinject
import Core
import Session
import DataPool
import Pool

private let ICON_SIZE: CGFloat = 24

struct PoolScoreListRouter: View {
    let user: AccountBundle
    let onPoolSelected: (PoolProfile) -> Void
    let onLogout: () -> Void

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var path = NavigationPath()
    @State private var drawerVisible = false

    var body: some View {
        PoolScoreListView(
            viewModel: PoolScoreListViewModel(
                getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                    poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                ),
                gamblerId: user.accountId
            ),
            onPoolDetailRequested: { pool in onPoolSelected(pool) }
        )
        .navigationBarItems(leading: navigationBarLeading(), trailing: navigationBarTrailing())
//        .navigationDestination(for: PoolFromLayoutCreatorRoute.self) { route in
//            PoolFromLayoutCreatorView(
//                viewModel: PoolFromLayoutCreatorViewModel(
//                    gamblerId: user.accountId,
//                    createPoolUseCase: diResolver.resolve(CreatePoolUseCase.self)!
//                ),
//                onPoolCreated: { _ in path = NavigationPath() },
//            )
//        }
        .drawer(isShowing: $drawerVisible) {
            PoolScoreListDrawerView(
                viewModel: PoolScoreListDrawerViewModel(logOutUseCase: diResolver.resolve(LogOutUseCase.self)!),
                onLogOut: onLogout,
            )
        }
    }

    private func navigationBarLeading() -> some View {
        Button(action: { drawerVisible.toggle() }) {
            Image(sharedResource: .menu)
                .resizable()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
                .tint(.primary)
        }
    }

    private func navigationBarTrailing() -> some View {
        Button(action: {
            path.append(PoolFromLayoutCreatorRoute())
        }) {
            Image(sharedResource: .filledAddCircle)
                .resizable()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
        }
    }
}
