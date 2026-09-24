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

private struct PoolScoreListRouterContent: View {
    let accountBundle: AccountBundle
    let onPoolSelect: (PoolProfile) -> Void
    let onSignOut: () -> Void
    @ObservedObject var poolScoreViewModel: PoolScoreListViewModel
    @StateObject private var drawerViewModel: PoolScoreListDrawerViewModel
    @StateObject private var usernameEditorViewModel: UsernameEditorViewModel

    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var navigation = DrawerHostNavigation()
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
        NavigationStack(path: $navigation.path) {
            PoolScoreListObservedView(
                viewModel: poolScoreViewModel,
                onPoolOpen: { pool in
                    // Opening a pool replaces the list rather than pushing a destination, so the
                    // check that `open` makes is made here.
                    if navigation.isHostInteractive {
                        onPoolSelect(pool)
                    }
                },
                onPoolCreate: { navigation.open(PoolFromLayoutCreatorRoute()) },
                onPoolLayoutSelect: { layoutId, layoutName in
                    navigation.open(PoolFromLayoutCreatorRoute(
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
            .toolbar {
                PlainToolbarItem(placement: .topBarLeading) {
                    navigationBarLeading()
                }
                PlainToolbarItem(placement: .topBarTrailing) {
                    navigationBarTrailing()
                }
            }
            .navigationDestination(for: PoolFromLayoutCreatorRoute.self) { route in
                PoolFromLayoutCreatorView(
                    viewModel: PoolFromLayoutCreatorViewModel(
                        gamblerId: accountBundle.accountId,
                        createPoolUseCase: diResolver.resolve(CreatePoolUseCase.self)!
                    ),
                    onPoolCreated: { _ in
                        wasPoolCreated = true
                        navigation.path = NavigationPath()
                    },
                    preselectedPoolLayoutId: route.preselectedPoolLayoutId,
                    preselectedPoolName: route.preselectedPoolName,
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
                    onEditUsername: { navigation.path.append(UsernameEditorRoute(accountId: accountBundle.accountId)) }
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
            PoolScoreListDrawerView(
                viewModel: drawerViewModel,
                onSignOut: {
                    if navigation.closeDrawerForChoice() {
                        drawerViewModel.logOut()
                        onSignOut()
                    }
                },
                onProfile: { navigation.openFromDrawer(ProfileRoute()) }
            )
        }
        .withParentGeometryProxy()
    }

    private func navigationBarLeading() -> some View {
        Button(action: { navigation.toggleDrawer() }) {
            AutoEmailAvatar()
                .navigationEmailAvatar()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(sharedResource: .openMenuAction))
        .drawerOpener()
    }

    private func navigationBarTrailing() -> some View {
        Button(action: {
            navigation.open(PoolFromLayoutCreatorRoute())
        }) {
            Image(sharedResource: .filledAddCircle)
                .resizable()
                .frame(width: createIconSize, height: createIconSize)
                .foregroundStyle(Color.accentColor)
        }
        .buttonStyle(.plain)
    }
}

private let navigationAvatarSize: CGFloat = 32
private let createIconSize: CGFloat = 48

private func poolScoreListFakeResolver() -> DIResolver {
    let container = Container()
    container.register(CreatePoolUseCase.self) { _ in
        CreatePoolUseCase(poolRepository: PoolFakePreviewRepository())
    }
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

#Preview("Light") {
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
    .preferredColorScheme(.light)
}

#Preview("Dark") {
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
    .preferredColorScheme(.dark)
}

#Preview("Store") {
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
    .environment(\.locale, .init(identifier: "es-CO"))
}
