import SwiftUI
import UI
import Swinject
import Core
import Session
import DataPool
import Pool

struct PoolScoreListRouter: View {
    let accountBundle: AccountBundle
    let onPoolSelect: (PoolProfile) -> Void
    let onSignOut: () -> Void
    @StateObject var poolScoreViewModel: PoolScoreListViewModel

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var path = NavigationPath()
    @State private var drawerVisible = false
    @State private var wasPoolCreated: Bool = false

    init(
        accountBundle: AccountBundle,
        onPoolSelect: @escaping (PoolProfile) -> Void,
        onSignOut: @escaping () -> Void,
        poolScoreViewModel: @escaping @autoclosure () -> PoolScoreListViewModel,
    ) {
        self.accountBundle = accountBundle
        self.onPoolSelect = onPoolSelect
        self.onSignOut = onSignOut
        self._poolScoreViewModel = .init(wrappedValue: poolScoreViewModel())
    }

    var body: some View {
        NavigationStack(path: $path) {
            PoolScoreListObservedView(
                viewModel: poolScoreViewModel,
                onPoolOpen: { pool in onPoolSelect(pool) },
                onPoolCreate: { path.append(PoolFromLayoutCreatorRoute()) }
            )
            .onAppear {
                if wasPoolCreated {
                    wasPoolCreated = false
                    poolScoreViewModel.refresh()
                }
            }
            .navigationBarItems(leading: navigationBarLeading(), trailing: navigationBarTrailing())
            .navigationDestination(for: PoolFromLayoutCreatorRoute.self) { route in
                PoolFromLayoutCreatorView(
                    viewModel: PoolFromLayoutCreatorViewModel(
                        gamblerId: accountBundle.accountId,
                        createPoolUseCase: diResolver.resolve(CreatePoolUseCase.self)!
                    ),
                    onPoolCreated: { _ in
                        wasPoolCreated = true
                        path = NavigationPath()
                    },
                )
            }
        }
        .drawer(isShowing: $drawerVisible) {
            PoolScoreListDrawerView(
                viewModel: PoolScoreListDrawerViewModel(logOutUseCase: diResolver.resolve(LogOutUseCase.self)!),
                onSignOut: onSignOut,
            )
        }
        .withParentGeometryProxy()
    }

    private func navigationBarLeading() -> some View {
        Button(action: { drawerVisible.toggle() }) {
            Image(sharedResource: .menu)
                .resizable()
                .frame(width: iconSize, height: iconSize)
                .tint(.primary)
        }
    }

    private func navigationBarTrailing() -> some View {
        Button(action: {
            path.append(PoolFromLayoutCreatorRoute())
        }) {
            Image(sharedResource: .filledAddCircle)
                .resizable()
                .frame(width: createIconSize, height: createIconSize)
        }
    }
}

private let iconSize: CGFloat = 24
private let createIconSize: CGFloat = 32
