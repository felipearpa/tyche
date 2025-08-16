import SwiftUI
import UI
import Core

struct PoolFromLayoutCreatorStepOneView : View {
    @StateObject private var viewModel: PoolFromLayoutCreatorStepOneViewModel
    let onNextClick: (CreatePoolModel) -> Void
    let createPoolModel: CreatePoolModel

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        viewModel: @autoclosure @escaping () -> PoolFromLayoutCreatorStepOneViewModel,
        createPoolModel: CreatePoolModel,
        onNextClick: @escaping (CreatePoolModel) -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.createPoolModel = createPoolModel
        self.onNextClick = onNextClick
    }

    var body: some View {
        PoolFromLayoutCreatorStepOneStatefulView(
            lazyPager: viewModel.lazyPager,
            fakeItemCount: 5,
            createPoolModel: createPoolModel,
            onNextClick: onNextClick,
        )
        .onAppearOnce {
            viewModel.lazyPager.refresh()
        }
    }
}

private struct PoolFromLayoutCreatorStepOneStatefulView : View {
    let lazyPager: LazyPager<String, PoolLayoutModel>
    let fakeItemCount: Int
    var createPoolModel: CreatePoolModel
    let onNextClick: (CreatePoolModel) -> Void

    @State private var selectedPoolLayout: PoolLayoutModel? = nil
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            HStack {
                Text(String(.choosePoolLayoutText))
                    .font(.title3)
                Spacer()
            }

            PoolFromLayoutCreatorList(
                lazyPager: lazyPager,
                fakeItemCount: fakeItemCount,
                selectedPoolLayout: selectedPoolLayout,
                onPoolLayoutChange: { newPoolLayout in selectedPoolLayout = newPoolLayout }
            )
        }
        .onChange(of: selectedPoolLayout) { newPoolLayout in
            guard let nonNullNewPoolLayout = newPoolLayout else { return }
            onNextClick(createPoolModel.copy { builder in builder.poolLayoutId = nonNullNewPoolLayout.id })
        }
    }
}

#Preview {
    struct PreviewWrapper: View {
        @Environment(\.boxSpacing) private var boxSpacing

        var body: some View {
            PoolFromLayoutCreatorStepOneStatefulView(
                lazyPager: LazyPager(
                    pagingData: PagingData(
                        pagingConfig: PagingConfig(prefetchDistance: 5),
                        pagingSourceFactory: OpenPoolLayoutPagingSource(
                            pagingQuery: { _ in
                                    .success(CursorPage(items: poolLayoutDummyModels(), next: nil))
                            }
                        )
                    )
                ),
                fakeItemCount: 5,
                createPoolModel: emptyCreatePoolModel(),
                onNextClick: { _ in },
            )
            .padding(boxSpacing.medium)
        }
    }

    return PreviewWrapper()
}
