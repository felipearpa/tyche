import SwiftUI
import UI

private let ICON_SIZE: CGFloat = 64

public struct PoolJoinerView: View {
    @StateObject private var viewModel: PoolJoinerViewModel
    private let poolId: String
    private let gamblerId: String
    private let onJoinPool: () -> Void
    private let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        viewModel: @autoclosure @escaping () -> PoolJoinerViewModel,
        poolId: String,
        gamblerId: String,
        onJoinPool: @escaping () -> Void,
        onAbort: @escaping () -> Void,
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.onJoinPool = onJoinPool
        self.onAbort = onAbort
    }

    public var body: some View {
        PoolJoinerContainerView(
            poolState: viewModel.poolState,
            joinPoolState: viewModel.joinPoolState,
            onJoinPool: { viewModel.joinPool(poolId: poolId, gamblerId: gamblerId) },
            onAbort: onAbort,
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
    let onAbort: () -> Void

    var body: some View {
        switch joinPoolState {
        case .initial:
            PoolJoinerContent(poolState: poolState, onJoinPool: onJoinPool, onAbort: onAbort)
        case .loading, .success:
            LoadingContainerView {
                PoolJoinerContent(poolState: poolState, onJoinPool: {}, onAbort: {})
            }
        case .failure(let error):
            PoolJoinerContainerFailure(
                exception: error.localizedErrorOrDefault(),
                onRetry: onJoinPool,
                onAbort: onAbort,
            )
        }
    }
}

private struct PoolJoinerContent: View {
    let poolState: LoadableViewState<PoolModel>
    let onJoinPool: () -> Void
    let onAbort: () -> Void

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

                VStack(spacing: boxSpacing.small) {
                    Button(action: onJoinPool) {
                        Text(String(.joinPoolAction))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)

                    Button(action: onAbort) {
                        Text(String(.goToMyPoolsAction))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        case .failure(let error):
            PoolLoaderContainerFailure(exception: error.localizedErrorOrDefault(), onAbort: onAbort)
        }
    }
}

private struct PoolJoinerContainerFailure: View {
    let exception: LocalizedError
    let onRetry: () -> Void
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            ErrorView(localizedError: exception)

            VStack(spacing: boxSpacing.small) {
                Button(action: onRetry) {
                    Text(String(sharedResource: .retryAction))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Button(action: onAbort) {
                    Text(String(.goToMyPoolsAction))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
    }
}

private struct PoolLoaderContainerFailure: View {
    let exception: LocalizedError
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            ErrorView(localizedError: exception)

            Button(action: onAbort) {
                Text(String(.goToMyPoolsAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
    }
}

#Preview("Join Pool Initial") {
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
                onAbort: {},
            )
            .padding(boxSpacing.medium)
        }
    }
    return PreviewWrapper()
}

#Preview("Join Pool Loading") {
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
                onAbort: {},
            )
            .padding(boxSpacing.medium)
        }
    }
    return PreviewWrapper()
}

#Preview("Join Pool Failure") {
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
                onAbort: {},
            )
            .padding(boxSpacing.medium)
        }
    }
    return PreviewWrapper()
}
