import SwiftUI
import UI
import Core

struct FinishedPoolGamblerBetList: View {
    var lazyPager: LazyPager<String, PoolGamblerBetModel>

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        PagingVStack(
            lazyPager: lazyPager,
            loadingContent: {
                FinishedPoolGamblerBetFakeList(count: 50)
            },
            loadingContentOnConcatenate: {
                FinishedPoolGamblerBetFakeItem()
                Divider()
            }
        ) { poolGamblerBet in
            VStack(spacing: 0) {
                if shouldShowHeader(for: poolGamblerBet) {
                    Text(poolGamblerBet.matchDateTime.toShortDateString())
                        .font(.title3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, boxSpacing.medium)
                        .padding(.top, boxSpacing.medium)
                }

                FinishedPoolGamblerBetItem(poolGamblerBet: poolGamblerBet)
                    .padding(boxSpacing.large)

                Divider()
            }
        }
    }

    // Stub for header grouping logic; to be implemented via state or grouping
    private func shouldShowHeader(for bet: PoolGamblerBetModel) -> Bool {
        // Implement logic for date comparison and header grouping
        return true
    }
}

private struct FinishedPoolGamblerBetFakeList: View {
    let count: Int
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ScrollView {
            LazyVStack(spacing: boxSpacing.medium) {
                ForEach(0..<count, id: \.self) { _ in
                    FinishedPoolGamblerBetFakeItem()
                    Divider()
                }
            }
        }
    }
}

private struct FinishedPoolGamblerBetFakeItem: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        FinishedPoolGamblerBetItem(poolGamblerBet: poolGamblerBetFakeModel())
            .shimmer()
            .padding(boxSpacing.large)
    }
}

#Preview {
    FinishedPoolGamblerBetList(
        lazyPager: LazyPager(
            pagingData: PagingData(
                pagingConfig: PagingConfig(prefetchDistance: 5),
                pagingSourceFactory: PoolGamblerBetPagingSource(
                    pagingQuery: { _ in .
                        success(CursorPage(items: poolGamblerBetDummyModels(), next: nil))
                    }
                )
            )
        )
    )
}
