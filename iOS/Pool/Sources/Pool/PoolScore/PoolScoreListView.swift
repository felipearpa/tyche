import SwiftUI
import Core
import UI
import DataPool

public struct PoolScoreListView: View {
    @StateObject private var viewModel: PoolScoreListViewModel
    private let onPoolOpen: (PoolProfile) -> Void
    private let onPoolCreate: () -> Void
    private let onPoolLayoutSelect: (String, String) -> Void
    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> PoolScoreListViewModel,
        onPoolOpen: @escaping (PoolProfile) -> Void,
        onPoolCreate: @escaping () -> Void,
        onPoolLayoutSelect: @escaping (String, String) -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onPoolOpen = onPoolOpen
        self.onPoolCreate = onPoolCreate
        self.onPoolLayoutSelect = onPoolLayoutSelect
    }

    public var body: some View {
        PoolScoreListObservedView(
            viewModel: viewModel,
            onPoolOpen: onPoolOpen,
            onPoolCreate: onPoolCreate,
            onPoolLayoutSelect: onPoolLayoutSelect,
        )
        .padding(boxSpacing.medium)
    }
}

public struct PoolScoreListObservedView: View {
    @ObservedObject private var viewModel: PoolScoreListViewModel
    private let onPoolOpen: (PoolProfile) -> Void
    private let onPoolCreate: () -> Void
    private let onPoolLayoutSelect: (String, String) -> Void

    @State private var poolUrl: ShareablePoolUrl? = nil

    public init(
        viewModel: PoolScoreListViewModel,
        onPoolOpen: @escaping (PoolProfile) -> Void,
        onPoolCreate: @escaping () -> Void,
        onPoolLayoutSelect: @escaping (String, String) -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel)
        self.onPoolOpen = onPoolOpen
        self.onPoolCreate = onPoolCreate
        self.onPoolLayoutSelect = onPoolLayoutSelect
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        PoolScoreList(
            lazyPagingItems: viewModel.lazyPagingItems,
            lazyPoolLayouts: viewModel.lazyPoolLayouts,
            onPoolOpen: { poolId in
                onPoolOpen(PoolProfile(poolId: poolId))
            },
            onPoolJoin: { poolId in
                poolUrl = ShareablePoolUrl(viewModel.createUrlForJoining(poolId: poolId))
            },
            onPoolLayoutSelect: { poolLayout in
                onPoolLayoutSelect(poolLayout.id, poolLayout.name)
            },
            onSeeAllTemplates: onPoolCreate,
        )
        .navigationTitle(String(.gamblerPoolListTitle))
        .sheet(item: $poolUrl) { poolUrl in
            ShareSheet(activityItems: [URL(string: poolUrl.id)!])
        }
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

private struct JoinPoolUrlTemplateFakeProvider: JoinPoolUrlTemplateProvider {
    func callAsFunction() -> String { "https://example.com/pools/{poolId}/join?gambler={gamblerId}" }
}

private class PoolLayoutFakePreviewRepository: PoolLayoutRepository {
    func getOpenPoolLayouts(next: String?, searchText: String?) async -> Result<CursorPage<PoolLayout>, Error> {
        .success(CursorPage(items: [], next: nil))
    }
}

#Preview {
    NavigationStack {
        PoolScoreListView(
            viewModel: PoolScoreListViewModel(
                getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                    poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
                ),
                getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase(
                    poolLayoutRepository: PoolLayoutFakePreviewRepository()
                ),
                joinPoolUrlTemplate: JoinPoolUrlTemplateFakeProvider(),
                gamblerId: "gambler-id"
            ),
            onPoolOpen: { _ in },
            onPoolCreate: {},
            onPoolLayoutSelect: { _, _ in },
        )
    }
}
