import SwiftUI
import Swinject
import Core
import UI
import Session
import DataPool
import DataBet
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
                currentAccountModel: diResolver.resolve(CurrentAccountModel.self)!
            ),
            usernameEditorViewModel: UsernameEditorViewModel(
                onSave: { [diResolver] username in
                    await diResolver.resolve(UpdateUsernameUseCase.self)!.execute(username: username)
                }
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
    @State private var navigation = DrawerHostNavigation()
    @State private var inviteUrl: ShareablePoolUrl?
    @StateObject private var drawerViewModel: PoolHomeDrawerViewModel
    @StateObject private var usernameEditorViewModel: UsernameEditorViewModel

    init(
        user: AccountBundle,
        pool: PoolProfile,
        onChangePool: @escaping () -> Void,
        onSignOut: @escaping () -> Void,
        drawerViewModel: @autoclosure @escaping () -> PoolHomeDrawerViewModel,
        usernameEditorViewModel: @autoclosure @escaping () -> UsernameEditorViewModel
    ) {
        self.user = user
        self.pool = pool
        self.onChangePool = onChangePool
        self.onSignOut = onSignOut
        self._drawerViewModel = StateObject(wrappedValue: drawerViewModel())
        self._usernameEditorViewModel = StateObject(wrappedValue: usernameEditorViewModel())
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
        NavigationStack(path: $navigation.path) {
            PoolHomeView(
                gamblerId: user.accountId,
                poolId: pool.poolId,
                onChangePool: {
                    // Switching pools replaces pool home rather than pushing a destination, so the
                    // check that `open` makes is made here.
                    if navigation.isHostInteractive {
                        onChangePool()
                    }
                },
                onMenuTap: { navigation.toggleDrawer() },
                onGamblerOpen: { tappedPoolId, tappedGamblerId, tappedGamblerUsername in
                    if tappedGamblerId != user.accountId {
                        navigation.open(
                            BetTimelineListViewRoute(
                                poolId: tappedPoolId,
                                gamblerId: tappedGamblerId,
                                gamblerUsername: tappedGamblerUsername
                            )
                        )
                    }
                },
                onMatchOpen: { poolId, gamblerId, matchId in
                    navigation.open(
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
                    onHome: { navigation.path = NavigationPath() },
                    onMatchOpen: { poolId, gamblerId, matchId in
                        navigation.path.append(
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
                    onHome: { navigation.path = NavigationPath() },
                    onGamblerOpen: { tappedPoolId, tappedGamblerId, tappedGamblerUsername in
                        if tappedGamblerId != user.accountId {
                            navigation.path.append(
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
            .navigationDestination(for: ManageGamblersRoute.self) { route in
                ManageGamblersListView(
                    viewModel: ManageGamblersListViewModel(
                        getPoolMembersUseCase: diResolver.resolve(GetPoolMembersUseCase.self)!,
                        removeGamblerUseCase: diResolver.resolve(RemoveGamblerUseCase.self)!,
                        poolId: route.poolId
                    )
                )
            }
            .navigationDestination(for: ProfileRoute.self) { _ in
                ProfileView(
                    viewModel: ProfileViewModel(
                        currentAccountModel: diResolver.resolve(CurrentAccountModel.self)!,
                        currentAccountCoordinator: diResolver.resolve(CurrentAccountCoordinator.self)!,
                        onUploadAvatar: { [diResolver] imageData in
                            await diResolver.resolve(UploadAvatarUseCase.self)!.execute(imageData: imageData)
                        }
                    ),
                    onEditUsername: { navigation.path.append(UsernameEditorRoute(accountId: user.accountId)) }
                )
            }
            .navigationDestination(for: UsernameEditorRoute.self) { _ in
                UsernameEditorDestination(
                    currentAccountModel: diResolver.resolve(CurrentAccountModel.self)!,
                    viewModel: usernameEditorViewModel,
                    onSaved: { _ in navigation.path.removeLast() }
                )
            }
        }
        .environment(\.diResolver, diResolver)
        // While a destination is shown, the drawer detaches its drags so the destination keeps
        // its native back button and back-swipe.
        .drawer(
            isShowing: $navigation.isDrawerOpen,
            allowsDragging: navigation.isHostVisible,
            stabilizesNavigationLayout: true
        ) {
            PoolHomeDrawerView(
                viewModel: drawerViewModel,
                onLogout: {
                    if navigation.closeDrawerForChoice() {
                        drawerViewModel.signOut()
                        onSignOut()
                    }
                },
                onInvite: {
                    if navigation.closeDrawerForChoice() {
                        let template = diResolver.resolve(JoinPoolUrlTemplateProvider.self)!
                        inviteUrl = ShareablePoolUrl(String(format: template(), pool.poolId))
                    }
                },
                onManageGamblers: { navigation.openFromDrawer(ManageGamblersRoute(poolId: pool.poolId)) },
                onPoolDeleting: {
                    navigation.isDrawerOpen = false
                },
                // Not guarded: a completed deletion reaches the pool list whatever is on screen.
                onPoolDeleted: {
                    navigation.isDrawerOpen = false
                    onChangePool()
                },
                onProfile: { navigation.openFromDrawer(ProfileRoute()) }
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

private func poolHomeFakeResolver() -> DIResolver {
    let container = Container()
    container.register(LogOutUseCase.self) { _ in
        LogOutUseCase.preview()
    }
    container.register(UpdateUsernameUseCase.self) { _ in
        UpdateUsernameUseCase.preview()
    }
    container.register(CurrentAccountCoordinator.self) { _ in
        CurrentAccountCoordinator.preview()
    }
    container.register(CurrentAccountModel.self) { _ in
        CurrentAccountModel.preview()
    }
    container.register(UploadAvatarUseCase.self) { _ in
        UploadAvatarUseCase.preview()
    }
    container.register(GetPoolGamblerScoreUseCase.self) { _ in
        GetPoolGamblerScoreUseCase(
            poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
        )
    }
    container.register(GetPoolUseCase.self) { _ in
        GetPoolUseCase(poolRepository: PoolFakePreviewRepository())
    }
    container.register(DeletePoolUseCase.self) { _ in
        DeletePoolUseCase(poolRepository: PoolFakePreviewRepository())
    }
    container.register(JoinPoolUrlTemplateProvider.self) { _ in
        JoinPoolUrlTemplateFakeProvider()
    }
    container.register(PoolGamblerScoreRepository.self) { _ in
        PoolGamblerScoreFakeRepository()
    }
    container.register(PoolGamblerBetRepository.self) { _ in
        PoolGamblerBetFakeRepository()
    }
    return DIResolver(resolver: container.synchronize())
}

private class PoolFakePreviewRepository: PoolRepository {
    func getPool(id: String) async -> Result<Pool, Error> {
        .success(Pool(id: id, name: "Preview Pool", creatorGamblerId: "gambler-id"))
    }
    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, Error> { .failure(NSError()) }
    func joinPool(joinPoolInput: JoinPoolInput) async -> Result<Void, Error> { .failure(NSError()) }
    func deletePool(poolId: String, gamblerId: String) async -> Result<Void, Error> { .success(()) }
}

private struct JoinPoolUrlTemplateFakeProvider: JoinPoolUrlTemplateProvider {
    func callAsFunction() -> String { "https://example.com/pools/{poolId}/join?gambler={gamblerId}" }
}

#Preview("Light") {
    PoolHomeRouter(
        user: AccountBundle(
            accountId: "gambler-id",
            externalAccountId: "external-id",
            email: "preview@example.com"
        ),
        pool: PoolProfile(poolId: "pool-id"),
        onChangePool: {},
        onSignOut: {}
    )
    .environment(\.diResolver, poolHomeFakeResolver())
    .preferredColorScheme(.light)
}

#Preview("Dark") {
    PoolHomeRouter(
        user: AccountBundle(
            accountId: "gambler-id",
            externalAccountId: "external-id",
            email: "preview@example.com"
        ),
        pool: PoolProfile(poolId: "pool-id"),
        onChangePool: {},
        onSignOut: {}
    )
    .environment(\.diResolver, poolHomeFakeResolver())
    .preferredColorScheme(.dark)
}

#Preview("Store") {
    PoolHomeRouter(
        user: AccountBundle(
            accountId: "gambler-id",
            externalAccountId: "external-id",
            email: "preview@example.com"
        ),
        pool: PoolProfile(poolId: "pool-id"),
        onChangePool: {},
        onSignOut: {}
    )
    .environment(\.diResolver, poolHomeFakeResolver())
    .environment(\.locale, .init(identifier: "es-CO"))
}
