import SwiftUI
import Core
import UI
import DataPool

public struct PoolScoreListView: View {
    @StateObject private var viewModel: PoolScoreListViewModel
    private let onPoolDetailRequested: (PoolProfile) -> Void
    @State private var poolUrl: ShareablePoolUrl? = nil

    public init(
        viewModel: @autoclosure @escaping () -> PoolScoreListViewModel,
        onPoolDetailRequested: @escaping (PoolProfile) -> Void)
    {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onPoolDetailRequested = onPoolDetailRequested
    }

    public var body: some View {
        let _ = Self._printChanges()

        PoolScoreList(
            lazyPager: viewModel.lazyPager,
            onPoolDetailRequested: { poolId in
                onPoolDetailRequested(PoolProfile(poolId: poolId))
            },
            onJoin: { poolId in
                poolUrl = ShareablePoolUrl(viewModel.createUrlForJoining(poolId: poolId))
            }
        )
        .navigationTitle(String(.gamblerPoolListTitle))
        .refreshable {
            viewModel.lazyPager.refresh()
        }
        .onAppearOnce {
            viewModel.lazyPager.refresh()
        }
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

#Preview {
    NavigationStack {
        PoolScoreListView(
            viewModel: PoolScoreListViewModel(
                getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                    poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
                ),
                gamblerId: "gambler-id"
            ),
            onPoolDetailRequested: { _ in }
        )
    }
}
