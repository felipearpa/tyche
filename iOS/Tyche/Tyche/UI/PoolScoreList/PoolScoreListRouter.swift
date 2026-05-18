import SwiftUI
import UI
import Swinject
import Core
import Session
import DataPool
import Pool
import Account

struct PoolScoreListRouter: View {
    let accountBundle: AccountBundle
    let onPoolSelect: (PoolProfile) -> Void
    let onSignOut: () -> Void
    @StateObject var poolScoreViewModel: PoolScoreListViewModel

    @Environment(\.diResolver) private var diResolver: DIResolver

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
        PoolScoreListRouterContent(
            accountBundle: accountBundle,
            onPoolSelect: onPoolSelect,
            onSignOut: onSignOut,
            poolScoreViewModel: poolScoreViewModel,
            drawerViewModel: PoolScoreListDrawerViewModel(
                logOutUseCase: diResolver.resolve(LogOutUseCase.self)!,
                accountStorage: diResolver.resolve(AccountStorage.self)!
            ),
            usernameEditorViewModel: UsernameEditorViewModel(
                updateUsernameUseCase: diResolver.resolve(UpdateUsernameUseCase.self)!
            )
        )
    }
}

private struct PoolScoreListRouterContent: View {
    let accountBundle: AccountBundle
    let onPoolSelect: (PoolProfile) -> Void
    let onSignOut: () -> Void
    @ObservedObject var poolScoreViewModel: PoolScoreListViewModel
    @StateObject private var drawerViewModel: PoolScoreListDrawerViewModel
    @StateObject private var usernameEditorViewModel: UsernameEditorViewModel

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var path = NavigationPath()
    @State private var drawerVisible = false
    @State private var isEditingAccount = false
    @State private var wasPoolCreated: Bool = false

    init(
        accountBundle: AccountBundle,
        onPoolSelect: @escaping (PoolProfile) -> Void,
        onSignOut: @escaping () -> Void,
        poolScoreViewModel: PoolScoreListViewModel,
        drawerViewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        usernameEditorViewModel: @autoclosure @escaping () -> UsernameEditorViewModel
    ) {
        self.accountBundle = accountBundle
        self.onPoolSelect = onPoolSelect
        self.onSignOut = onSignOut
        self.poolScoreViewModel = poolScoreViewModel
        self._drawerViewModel = StateObject(wrappedValue: drawerViewModel())
        self._usernameEditorViewModel = StateObject(wrappedValue: usernameEditorViewModel())
    }

    var body: some View {
        NavigationStack(path: $path) {
            PoolScoreListObservedView(
                viewModel: poolScoreViewModel,
                onPoolOpen: { pool in onPoolSelect(pool) },
                onPoolCreate: { path.append(PoolFromLayoutCreatorRoute()) },
                onPoolLayoutSelect: { layoutId, layoutName in
                    path.append(PoolFromLayoutCreatorRoute(
                        preselectedPoolLayoutId: layoutId,
                        preselectedPoolName: layoutName,
                    ))
                },
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
                    preselectedPoolLayoutId: route.preselectedPoolLayoutId,
                    preselectedPoolName: route.preselectedPoolName,
                )
            }
        }
        .drawer(isShowing: $drawerVisible) {
            PoolScoreListDrawerView(
                viewModel: drawerViewModel,
                onSignOut: onSignOut,
                onEditAccount: { isEditingAccount = true }
            )
        }
        .withParentGeometryProxy()
        .minimalDialog(isPresented: $isEditingAccount) {
            UsernameEditor(
                initialUsername: drawerViewModel.username,
                viewModel: usernameEditorViewModel,
                onSaved: { newUsername in
                    drawerViewModel.applyUsername(newUsername)
                    isEditingAccount = false
                },
                onDismiss: {
                    isEditingAccount = false
                }
            )
        }
    }

    private func navigationBarLeading() -> some View {
        Button(action: { drawerVisible.toggle() }) {
            AutoEmailAvatar()
                .navigationEmailAvatar()
        }
        .buttonStyle(.plain)
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

private let navigationAvatarSize: CGFloat = 32
private let createIconSize: CGFloat = 32

private func poolScoreListFakeResolver() -> DIResolver {
    let container = Container()
    container.register(CreatePoolUseCase.self) { _ in
        CreatePoolUseCase(poolRepository: PoolFakePreviewRepository())
    }
    return DIResolver(resolver: container.synchronize())
}

private class PoolFakePreviewRepository: PoolRepository {
    func getPool(id: String) async -> Result<Pool, Error> { .failure(NSError()) }
    func createPool(createPoolInput: CreatePoolInput) async -> Result<CreatePoolOutput, Error> { .failure(NSError()) }
    func joinPool(joinPoolInput: JoinPoolInput) async -> Result<Void, Error> { .failure(NSError()) }
    func deletePool(poolId: String, gamblerId: String) async -> Result<Void, Error> { .failure(NSError()) }
}

private class PoolLayoutFakePreviewRepository: PoolLayoutRepository {
    func getOpenPoolLayouts(next: String?, searchText: String?) async -> Result<CursorPage<PoolLayout>, Error> {
        .success(CursorPage(items: [], next: nil))
    }
}

private struct JoinPoolUrlTemplateFakeProvider: JoinPoolUrlTemplateProvider {
    func callAsFunction() -> String { "https://example.com/pools/{poolId}/join?gambler={gamblerId}" }
}

#Preview {
    PoolScoreListRouter(
        accountBundle: AccountBundle(
            accountId: "gambler-id",
            externalAccountId: "external-id",
            email: "preview@example.com"
        ),
        onPoolSelect: { _ in },
        onSignOut: {},
        poolScoreViewModel: PoolScoreListViewModel(
            getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
            ),
            getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase(
                poolLayoutRepository: PoolLayoutFakePreviewRepository()
            ),
            joinPoolUrlTemplate: JoinPoolUrlTemplateFakeProvider(),
            gamblerId: "gambler-id"
        )
    )
    .environment(\.diResolver, poolScoreListFakeResolver())
}
