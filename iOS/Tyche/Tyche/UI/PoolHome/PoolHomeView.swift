import SwiftUI
import Swinject
import Core
import UI
import DataPool
import Pool
import DataBet
import Bet
import Session

struct PoolHomeView: View {
    let gamblerId: String
    let poolId: String
    let onChangePool: () -> Void
    let onSignOut: () -> Void
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) var diResolver: DIResolver

    init(
        gamblerId: String,
        poolId: String,
        onChangePool: @escaping () -> Void,
        onSignOut: @escaping () -> Void,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.gamblerId = gamblerId
        self.poolId = poolId
        self.onChangePool = onChangePool
        self.onSignOut = onSignOut
        self.onGamblerOpen = onGamblerOpen
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        PoolHomeContent(
            gamblerId: gamblerId,
            poolId: poolId,
            onChangePool: onChangePool,
            onSignOut: onSignOut,
            onGamblerOpen: onGamblerOpen,
            onMatchOpen: onMatchOpen,
            drawerViewModel: PoolHomeDrawerViewModel(
                poolId: poolId,
                gamblerId: gamblerId,
                logoutUseCase: diResolver.resolve(LogOutUseCase.self)!,
                getPoolGamblerScoreUseCase: diResolver.resolve(GetPoolGamblerScoreUseCase.self)!,
                getPoolUseCase: diResolver.resolve(GetPoolUseCase.self)!,
                deletePoolUseCase: diResolver.resolve(DeletePoolUseCase.self)!,
                accountStorage: diResolver.resolve(AccountStorage.self)!
            )
        )
    }
}

private struct PoolHomeContent: View {
    let gamblerId: String
    let poolId: String
    let onChangePool: () -> Void
    let onSignOut: () -> Void
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.diResolver) var diResolver: DIResolver
    @StateObject private var drawerViewModel: PoolHomeDrawerViewModel
    @State private var selectedTab = PoolHomeTab.gamblerScores
    @State private var drawerVisible = false
    @State private var inviteUrl: ShareablePoolUrl?

    init(
        gamblerId: String,
        poolId: String,
        onChangePool: @escaping () -> Void,
        onSignOut: @escaping () -> Void,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?,
        onMatchOpen: MatchOpenHandler?,
        drawerViewModel: @autoclosure @escaping () -> PoolHomeDrawerViewModel
    ) {
        self.gamblerId = gamblerId
        self.poolId = poolId
        self.onChangePool = onChangePool
        self.onSignOut = onSignOut
        self.onGamblerOpen = onGamblerOpen
        self.onMatchOpen = onMatchOpen
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
        TabView(selection: $selectedTab) {
            GamblerScoreListView(
                viewModel: GamblerScoreListViewModel(
                    getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId
                ),
                onGamblerOpen: onGamblerOpen
            )
            .tag(PoolHomeTab.gamblerScores)
            .tabItem {
                Label(
                    title: { Text(.scoreTab) },
                    icon: { Image(.sportScore) }
                )
            }

            PendingBetListView(
                viewModel: PendingBetListViewModel(
                    getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId
                ),
                onMatchOpen: onMatchOpen
            )
            .tag(PoolHomeTab.bets)
            .tabItem {
                Label(
                    title: { Text(.betTab) },
                    icon: { Image(.money) }
                )
            }

            FinishedBetListView(
                viewModel: FinishedBetListViewModel(
                    getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    gamblerId: gamblerId,
                    poolId: poolId,
                ),
                onMatchOpen: onMatchOpen
            )
            .tag(PoolHomeTab.historyBet)
            .tabItem {
                Label(
                    title: { Text(.historyBetsTab) },
                    icon: { Image(.money) }
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
                    inviteUrl = ShareablePoolUrl(String(format: template(), poolId))
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
        .drawerStyle(.liquidGlass)
        .navigationTitle(selectedTab.title)
        .navigationBarItems(
            leading: navigationBarLeading(),
            trailing: navigationBarTrailing()
        )
        .sheet(item: $inviteUrl) { inviteUrl in
            ShareSheet(activityItems: [URL(string: inviteUrl.id)!])
                .presentationDetents([.medium, .large])
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
        Button(action: onChangePool) {
            Image(systemName: "arrow.left.arrow.right")
                .resizable()
                .scaledToFit()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
                .tint(.primary)
        }
    }
}

private enum PoolHomeTab: Int {
    case gamblerScores
    case bets
    case historyBet
}

private extension PoolHomeTab {
    var title: String {
        switch self {
        case .gamblerScores:
            return String(localized: .scoreTab)
        case .bets:
            return String(localized: .betTab)
        case .historyBet:
            return String(localized: .historyBetsTab)
        }
    }
}

private let ICON_SIZE: CGFloat = 24

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

#Preview {
    let diResolver = DIResolver(
        resolver:Assembler([
            PoolHomeAssembler()
        ]).resolver)

    PoolHomeView(
        gamblerId: "gambler-id",
        poolId: "pool-id",
        onChangePool: {},
        onSignOut: {}
    )
    .environment(\.diResolver, diResolver)
}

private class PoolHomeAssembler: Assembly {
    func assemble(container: Container) {
        container.register(PoolGamblerScoreRepository.self) { _ in
            PoolGamblerScoreFakeRepository()
        }

        container.register(PoolGamblerBetRepository.self) { _ in
            PoolGamblerBetFakeRepository()
        }
    }
}
