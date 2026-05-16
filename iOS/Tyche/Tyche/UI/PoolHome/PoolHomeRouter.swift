import SwiftUI
import Swinject
import Core
import UI
import Session
import DataPool
import Bet
import Pool

struct PoolHomeRouter: View {
    let user: AccountBundle
    let pool: PoolProfile
    let onChangePool: () -> Void
    let onSignOut: () -> Void

    @Environment(\.diResolver) private var diResolver: DIResolver

    var body: some View {
        PoolHomeRouterContent(
            user: user,
            pool: pool,
            onChangePool: onChangePool,
            onSignOut: onSignOut,
            drawerViewModel: PoolHomeDrawerViewModel(
                poolId: pool.poolId,
                gamblerId: user.accountId,
                logoutUseCase: diResolver.resolve(LogOutUseCase.self)!,
                getPoolGamblerScoreUseCase: diResolver.resolve(GetPoolGamblerScoreUseCase.self)!,
                getPoolUseCase: diResolver.resolve(GetPoolUseCase.self)!,
                deletePoolUseCase: diResolver.resolve(DeletePoolUseCase.self)!,
                accountStorage: diResolver.resolve(AccountStorage.self)!
            )
        )
    }
}

private struct PoolHomeRouterContent: View {
    let user: AccountBundle
    let pool: PoolProfile
    let onChangePool: () -> Void
    let onSignOut: () -> Void

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var path = NavigationPath()
    @State private var drawerVisible = false
    @State private var inviteUrl: ShareablePoolUrl?
    @StateObject private var drawerViewModel: PoolHomeDrawerViewModel

    init(
        user: AccountBundle,
        pool: PoolProfile,
        onChangePool: @escaping () -> Void,
        onSignOut: @escaping () -> Void,
        drawerViewModel: @autoclosure @escaping () -> PoolHomeDrawerViewModel
    ) {
        self.user = user
        self.pool = pool
        self.onChangePool = onChangePool
        self.onSignOut = onSignOut
        self._drawerViewModel = StateObject(wrappedValue: drawerViewModel())
    }

    var body: some View {
        Group {
            if drawerViewModel.deleteState.isLoading() {
                LoadingContainerView { mainContent }
            } else {
                mainContent
            }
        }
        .errorAlert(
            Binding(
                get: { drawerViewModel.deleteState.localizedErrorOrNil() },
                set: { _ in drawerViewModel.resetDeleteState() }
            ),
            onDismiss: {}
        )
    }

    @ViewBuilder
    private var mainContent: some View {
        NavigationStack(path: $path) {
            PoolHomeView(
                gamblerId: user.accountId,
                poolId: pool.poolId,
                onChangePool: onChangePool,
                onMenuTap: { drawerVisible.toggle() },
                onGamblerOpen: { tappedPoolId, tappedGamblerId, tappedGamblerUsername in
                    if tappedGamblerId != user.accountId {
                        path.append(
                            BetTimelineListViewRoute(
                                poolId: tappedPoolId,
                                gamblerId: tappedGamblerId,
                                gamblerUsername: tappedGamblerUsername
                            )
                        )
                    }
                },
                onMatchOpen: { poolId, gamblerId, matchId in
                    path.append(
                        MatchBetListViewRoute(
                            poolId: poolId,
                            gamblerId: gamblerId,
                            matchId: matchId
                        )
                    )
                }
            )
            .navigationDestination(for: BetTimelineListViewRoute.self) { route in
                BetTimelineListView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    gamblerUsername: route.gamblerUsername,
                    onHome: { path = NavigationPath() },
                    onMatchOpen: { poolId, gamblerId, matchId in
                        path.append(
                            MatchBetListViewRoute(
                                poolId: poolId,
                                gamblerId: gamblerId,
                                matchId: matchId
                            )
                        )
                    }
                )
            }
            .navigationDestination(for: MatchBetListViewRoute.self) { route in
                MatchBetListView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    matchId: route.matchId,
                    onHome: { path = NavigationPath() },
                    onGamblerOpen: { tappedPoolId, tappedGamblerId, tappedGamblerUsername in
                        if tappedGamblerId != user.accountId {
                            path.append(
                                BetTimelineListViewRoute(
                                    poolId: tappedPoolId,
                                    gamblerId: tappedGamblerId,
                                    gamblerUsername: tappedGamblerUsername
                                )
                            )
                        }
                    }
                )
            }
        }
        .drawer(isShowing: $drawerVisible) {
            PoolHomeDrawerView(
                viewModel: drawerViewModel,
                onLogout: {
                    drawerVisible = false
                    onSignOut()
                },
                onInvite: {
                    drawerVisible = false
                    let template = diResolver.resolve(JoinPoolUrlTemplateProvider.self)!
                    inviteUrl = ShareablePoolUrl(String(format: template(), pool.poolId))
                },
                onPoolDeleting: {
                    drawerVisible = false
                },
                onPoolDeleted: {
                    drawerVisible = false
                    onChangePool()
                }
            )
        }
        .sheet(item: $inviteUrl) { inviteUrl in
            ShareSheet(activityItems: [URL(string: inviteUrl.id)!])
                .presentationDetents([.medium, .large])
        }
        .withParentGeometryProxy()
    }
}

private struct ShareablePoolUrl: Identifiable {
    let id: String

    init(_ id: String) {
        self.id = id
    }
}

private struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
