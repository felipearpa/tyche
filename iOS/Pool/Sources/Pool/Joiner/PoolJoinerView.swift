import SwiftUI
import UI

public struct PoolJoinerView: View {
    @StateObject private var viewModel: PoolJoinerViewModel
    private let poolId: String
    private let gamblerId: String
    private let onJoinPool: () -> Void
    private let onAbort: () -> Void

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
        let joinPool = { viewModel.joinPool(poolId: poolId, gamblerId: gamblerId) }

        PoolJoinerStatefulView(
            poolState: viewModel.poolState,
            joinPoolState: viewModel.joinPoolState,
            onJoinPool: joinPool,
            onAbort: onAbort,
        )
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

private struct PoolJoinerStatefulView: View {
    let poolState: LoadableViewState<PoolModel>
    let joinPoolState: LoadableViewState<Void>
    let onJoinPool: () -> Void
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        switch joinPoolState {
        case .initial:
            PoolLoadStatefulView(poolState: poolState, onJoinPool: onJoinPool, onAbort: onAbort)
        case .loading, .success:
            LoadingContainerView {
                PoolLoadStatefulView(poolState: poolState, onJoinPool: {}, onAbort: {})
            }
        case .failure(let error):
            JoinFailureContent(
                localizedError: error.localizedErrorOrDefault(),
                onRetry: onJoinPool,
                onAbort: onAbort,
            )
            .padding(boxSpacing.medium)
        }
    }
}

private struct PoolLoadStatefulView: View {
    let poolState: LoadableViewState<PoolModel>
    let onJoinPool: () -> Void
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        switch poolState {
        case .initial, .loading:
            LoadingContainerView { EmptyView() }
        case .success(let pool):
            SuccessContent(pool: pool, onJoinPool: onJoinPool, onAbort: onAbort)
                .padding(boxSpacing.medium)
        case .failure(let error):
            LoadFailureContent(
                localizedError: error.localizedErrorOrDefault(),
                onAbort: onAbort,
            )
            .padding(boxSpacing.medium)
        }
    }
}

private struct SuccessContent: View {
    let pool: PoolModel
    let onJoinPool: () -> Void
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: boxSpacing.large) {
                Image(.emojiPeople)
                    .resizable()
                    .frame(width: ICON_SIZE, height: ICON_SIZE)

                VStack(spacing: boxSpacing.medium) {
                    Text(String(.readyToJoinTitle))
                        .multilineTextAlignment(.center)
                        .font(.title)

                    Text(String(.readyToJoinSubtitle))
                        .multilineTextAlignment(.center)
                }

                PoollPill(pool: pool)
            }

            Spacer()

            VStack(spacing: boxSpacing.small) {
                Button(action: onJoinPool) {
                    Text(String(.joinPoolAction))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.liquidGlassProminent)

                Button(action: onAbort) {
                    Text(String(.goToMyPoolsAction))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.liquidGlass)
            }
            .padding(.vertical, boxSpacing.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct PoollPill: View {
    let pool: PoolModel

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        Label {
            Text(pool.name)
        } icon: {
            Image(sharedResource: .trophy)
                .resizable()
                .frame(width: PILL_ICON_SIZE, height: PILL_ICON_SIZE)
        }
        .font(.subheadline)
        .padding(.horizontal, boxSpacing.medium)
        .padding(.vertical, boxSpacing.small)
        .background(Color(sharedResource: .primaryContainer))
        .foregroundStyle(Color(sharedResource: .onPrimaryContainter))
        .clipShape(RoundedRectangle(cornerRadius: boxSpacing.large))
    }
}

private struct JoinFailureContent: View {
    let localizedError: LocalizedError
    let onRetry: () -> Void
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            ErrorView(localizedError: localizedError)

            Spacer()

            VStack(spacing: boxSpacing.small) {
                if !(localizedError is JoinPoolLocalizedError) {
                    Button(action: onRetry) {
                        Text(String(sharedResource: .retryAction))
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.liquidGlassProminent)
                }

                Button(action: onAbort) {
                    Text(String(.goToMyPoolsAction))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.liquidGlass)
            }
            .padding(.vertical, boxSpacing.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct LoadFailureContent: View {
    let localizedError: LocalizedError
    let onAbort: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            ErrorView(localizedError: localizedError)

            Spacer()

            Button(action: onAbort) {
                Text(String(.goToMyPoolsAction))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.liquidGlassProminent)
            .padding(.vertical, boxSpacing.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private let ICON_SIZE: CGFloat = 64
private let PILL_ICON_SIZE: CGFloat = 16

#Preview("initial") {
    PoolJoinerStatefulView(
        poolState: .success(PoolModel(id: "id", name: "American Cup 2024")),
        joinPoolState: .initial,
        onJoinPool: {},
        onAbort: {},
    )
}

#Preview("loading") {
    PoolJoinerStatefulView(
        poolState: .success(PoolModel(id: "id", name: "American Cup 2024")),
        joinPoolState: .loading,
        onJoinPool: {},
        onAbort: {},
    )
}

#Preview("join failure") {
    PoolJoinerStatefulView(
        poolState: .success(PoolModel(id: "id", name: "American Cup 2024")),
        joinPoolState: .failure(UnknownLocalizedError()),
        onJoinPool: {},
        onAbort: {},
    )
}

#Preview("load failure") {
    PoolJoinerStatefulView(
        poolState: .failure(UnknownLocalizedError()),
        joinPoolState: .initial,
        onJoinPool: {},
        onAbort: {},
    )
}
