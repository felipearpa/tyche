import SwiftUI
import UI

private let ICON_SIZE: CGFloat = 64

public struct PoolJoinerView: View {
    @StateObject private var viewModel: PoolJoinerViewModel
    private let poolId: String
    private let gamblerId: String
    private let onJoinPool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> PoolJoinerViewModel,
        poolId: String,
        gamblerId: String,
        onJoinPool: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.onJoinPool = onJoinPool
    }

    public var body: some View {
        PoolJoinerContainerView(
            poolState: viewModel.poolState,
            joinPoolState: viewModel.joinPoolState,
            onJoinPool: { viewModel.joinPool(poolId: poolId, gamblerId: gamblerId) }
        )
        .padding(boxSpacing.medium)
        .onAppearOnce {
            viewModel.loadPool(poolId: poolId)
        }
        .onChange(of: viewModel.joinPoolState) { state in
            if state.isSuccess() {
                onJoinPool()
            }
        }
    }
}

private struct PoolJoinerContainerView: View {
    let poolState: LoadableViewState<PoolModel>
    let joinPoolState: LoadableViewState<Void>
    let onJoinPool: () -> Void

    var body: some View {
        switch joinPoolState {
        case .initial:
            PoolJoinerContent(poolState: poolState, onJoinPool: onJoinPool)
        case .loading, .success:
            LoadingContainerView {
                PoolJoinerContent(poolState: poolState, onJoinPool: {})
            }
        case .failure(let error):
            PoolJoinerContainerFailure(
                exception: error.localizedErrorOrDefault(),
                onRetry: onJoinPool
            )
        }
    }
}

private struct PoolJoinerContent: View {
    let poolState: LoadableViewState<PoolModel>
    let onJoinPool: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        switch poolState {
        case .initial, .loading:
            LoadingContainerView { EmptyView() }
        case .success(let pool):
            VStack(spacing: boxSpacing.large) {
                Image(.emojiPeople)
                    .resizable()
                    .frame(width: ICON_SIZE, height: ICON_SIZE)

                VStack(spacing: boxSpacing.medium) {
                    Text(String(format: String(.readyToJoinTitle), pool.name))
                        .font(.title3)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)

                    Text(String(format: String(.readyToJoinSubtitle), pool.name))
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }

                Button(action: onJoinPool) {
                    Text(String(.joinPoolAction))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        case .failure(let error):
            ErrorView(localizedError: error.localizedErrorOrDefault())
        }
    }
}

private struct PoolJoinerContainerFailure: View {
    let exception: LocalizedError
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            ErrorView(localizedError: exception)

            Button(action: onRetry) {
                Text(String(sharedResource: .retryAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
    }
}

#Preview {
    struct PreviewWrapper: View {
        @Environment(\.boxSpacing) private var boxSpacing

        var body: some View {
            PoolJoinerContainerView(
                poolState: LoadableViewState.success(
                    PoolModel(
                        id: "id",
                        name: "American Cup 2024",
                    ),
                ),
                joinPoolState: LoadableViewState.initial,
                onJoinPool: {},
            )
            .padding(boxSpacing.medium)
        }
    }
    return PreviewWrapper()
}

#Preview {
    struct PreviewWrapper: View {
        @Environment(\.boxSpacing) private var boxSpacing

        var body: some View {
            PoolJoinerContainerView(
                poolState: LoadableViewState.success(
                    PoolModel(
                        id: "id",
                        name: "American Cup 2024",
                    ),
                ),
                joinPoolState: LoadableViewState.loading,
                onJoinPool: {},
            )
            .padding(boxSpacing.medium)
        }
    }
    return PreviewWrapper()
}

#Preview {
    struct PreviewWrapper: View {
        @Environment(\.boxSpacing) private var boxSpacing

        var body: some View {
            PoolJoinerContainerView(
                poolState: LoadableViewState.success(
                    PoolModel(
                        id: "id",
                        name: "American Cup 2024",
                    ),
                ),
                joinPoolState: LoadableViewState.failure(UnknownLocalizedError()),
                onJoinPool: {},
            )
            .padding(boxSpacing.medium)
        }
    }
    return PreviewWrapper()
}
