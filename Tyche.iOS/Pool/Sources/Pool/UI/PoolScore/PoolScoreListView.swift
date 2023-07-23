import SwiftUI
import Core
import UI

private let defaultDebounceTimeInMilliseconds = 700

public struct PoolScoreListView: View {
    @ObservedObject private var viewModel: PoolScoreListViewModel
    @StateObject var searchDebounceText = DebounceString(dueTime: .milliseconds(defaultDebounceTimeInMilliseconds))
    var lazyPager: LazyPager<String, PoolGamblerScoreModel>
    
    public init(viewModel: PoolScoreListViewModel) {
        self.viewModel = viewModel
        self.lazyPager = LazyPager(pagingData: viewModel.pagingData)
    }
    
    public var body: some View {
        PoolScoreList(lazyPager: lazyPager)
            .navigationTitle(StringScheme.gamblerPoolListTitle.localizedString)
            .padding(8)
            .searchable(
                text: $searchDebounceText.text,
                prompt: UI.StringScheme.searchingLabel.localizedString
            )
            .refreshable {
                lazyPager.refresh()
            }
            .onChange(of: searchDebounceText.debouncedText) { newSearchText in
                viewModel.search(newSearchText)
            }
    }
}

struct PoolScoreListView_Previews: PreviewProvider {
    private class PoolGamblerScoreFakeRepository: PoolGamblerScoreRepository {
        func getPoolGamblerScoresByGambler(
            gamblerId: String,
            next: String?,
            searchText: String?
        ) async -> Result<CursorPage<PoolGamblerScore>, Error> {
            .success(
                CursorPage(
                    items: poolGamblerScores(),
                    next: nil
                )
            )
        }
    }
    
    static var previews: some View {
        NavigationStack {
            PoolScoreListView(
                viewModel: PoolScoreListViewModel(
                    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                        poolGamblerScoreRepository: PoolGamblerScoreFakeRepository()
                    ),
                    gamblerId: "gambler-id"
                )
            )
        }
    }
}
