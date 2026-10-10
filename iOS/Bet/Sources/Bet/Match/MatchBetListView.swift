import Core
import DataBet
import SwiftUI
import UI

/// The match screen: the match heading, then either the hidden-bets explanation with the
/// viewer's own prediction, or every participant's prediction in pool-total order.
///
/// `gamblerId` is the gambler whose entry opened the screen and can be another participant;
/// `viewerAccountId` is the signed-in account, which owns "Your bet" and the "You" row.
public struct MatchBetListView: View {
    let poolId: String
    let gamblerId: String
    let matchId: String
    let viewerAccountId: String
    let onHome: (() -> Void)?
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    @Environment(\.diResolver) var diResolver: DIResolver

    public init(
        poolId: String,
        gamblerId: String,
        matchId: String,
        viewerAccountId: String,
        onHome: (() -> Void)? = nil,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.matchId = matchId
        self.viewerAccountId = viewerAccountId
        self.onHome = onHome
        self.onGamblerOpen = onGamblerOpen
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        MatchBetListContent(
            viewModel: MatchBetListViewModel(
                getPoolGamblerBetUseCase: GetPoolGamblerBetUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                getRankedPoolMatchGamblerBetsUseCase: GetRankedPoolMatchGamblerBetsUseCase(
                    poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                ),
                poolId: poolId,
                gamblerId: gamblerId,
                matchId: matchId,
                viewerAccountId: viewerAccountId
            ),
            onGamblerOpen: onGamblerOpen
        )
        .navigationTitle(String(localized: .matchBetsViewTitle))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarItems(trailing: navigationBarTrailing())
    }

    @ViewBuilder
    private func navigationBarTrailing() -> some View {
        if let onHome {
            Button(action: onHome) {
                Image(sharedResource: .home)
                    .resizable()
                    .scaledToFit()
                    .frame(width: HOME_ICON_SIZE, height: HOME_ICON_SIZE)
                    .tint(.primary)
            }
            .accessibilityLabel(Text(sharedResource: .goHomeAction))
        }
    }
}

private let HOME_ICON_SIZE: CGFloat = 24

private struct MatchBetListContent: View {
    @StateObject private var viewModel: MatchBetListViewModel
    let onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)?

    init(
        viewModel: @autoclosure @escaping () -> MatchBetListViewModel,
        onGamblerOpen: ((_ poolId: String, _ gamblerId: String, _ gamblerUsername: String) -> Void)? = nil
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onGamblerOpen = onGamblerOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        Group {
            switch viewModel.heading.presentation {
            case .placeholder:
                MatchHeadingPlaceholderContent()
            case .failed:
                MatchHeadingFailureContent(onRetry: { Task { await viewModel.load() } })
            case .loaded(let match, .hidden, let status):
                MatchHiddenScrollContent(
                    header: { heading(match, .hidden, status) },
                    content: {
                        MatchHiddenBetsContent(
                            match: match,
                            ownBet: viewModel.ownBet,
                            onOwnBetRetry: { Task { await viewModel.loadOwnBet() } }
                        )
                    }
                )
                .refreshable { await viewModel.refresh() }
            case .loaded(let match, let presentation, let status):
                MatchBetList(
                    lazyPagingItems: viewModel.lazyPager,
                    viewerAccountId: viewModel.viewerAccountId,
                    isComputed: presentation == .computed,
                    onGamblerOpen: onGamblerOpen,
                    onRetryFirstPage: { Task { await viewModel.retryFirstPage() } },
                    onRetryPage: { error in Task { await viewModel.retryPage(after: error) } }
                ) {
                    heading(match, presentation, status)
                }
                .refreshable { await viewModel.refresh() }
            }
        }
        .task { await viewModel.load() }
    }

    private func heading(
        _ match: PoolGamblerBetModel,
        _ presentation: MatchBetsPresentation,
        _ status: MatchHeadingState.RefreshStatus
    ) -> some View {
        VStack(spacing: 12) {
            MatchHeader(match: match, presentation: presentation)
            if status == .failed {
                MatchHeadingRefreshFailure(onRetry: { Task { await viewModel.refresh() } })
            }
        }
    }
}

/// The heading's placeholder while the gate is unknown. No participant content is shown, not
/// even placeholders, until the gate is confirmed.
private struct MatchHeadingPlaceholderContent: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        ScrollView {
            MatchHeader(match: matchHeaderPlaceholderModel(), presentation: .hidden, isPlaceholder: true)
                .historyHorizontalGutter()
                .padding(.vertical, boxSpacing.medium)
        }
    }
}

/// The first heading request failed: nothing about the match is known, so the screen offers
/// only a retry.
private struct MatchHeadingFailureContent: View {
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        GeometryReader { proxy in
            ScrollView {
                VStack(spacing: boxSpacing.medium) {
                    ErrorView(localizedError: NetworkLocalizedError.remoteCommunication)
                    Button(action: onRetry) {
                        Text(sharedResource: .retryAction)
                    }
                    .buttonStyle(.standardProminent)
                }
                .padding(.all, boxSpacing.large)
                .frame(maxWidth: .infinity, minHeight: proxy.size.height)
            }
        }
    }
}

/// A failed refresh keeps the last confirmed heading and says so, with a retry.
private struct MatchHeadingRefreshFailure: View {
    let onRetry: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(.matchBetsHeadingRefreshFailure)
                .font(.subheadline)
                .foregroundStyle(HistoryStyle.secondaryText)
                .fixedSize(horizontal: false, vertical: true)
            HistoryPointsRetryButton(action: onRetry)
        }
    }
}

/// The hidden state's scrolling composition: the heading at the top, then the explanation
/// centered in the visible space left below it. When text is too large to fit, the whole content
/// grows and scrolls.
private struct MatchHiddenScrollContent<Header: View, Content: View>: View {
    @ViewBuilder let header: Header
    @ViewBuilder let content: Content

    @Environment(\.boxSpacing) private var boxSpacing
    @State private var visibleHeight: CGFloat = 0

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                Spacer(minLength: boxSpacing.large * 2)
                content
                Spacer(minLength: boxSpacing.large * 2)
            }
            .historyHorizontalGutter()
            .padding(.vertical, boxSpacing.medium)
            .frame(minHeight: visibleHeight)
        }
        .background(
            // The reader's size excludes the bars the scroll view extends under, so it is the
            // visible height.
            GeometryReader { proxy in
                Color.clear
                    .onAppear { visibleHeight = proxy.size.height }
                    .onChange(of: proxy.size.height) { height in visibleHeight = height }
            }
        )
    }
}

#Preview("Fake repository") {
    NavigationStack {
        MatchBetListView(
            poolId: "pool-id",
            gamblerId: "gambler-id",
            matchId: "match-id",
            viewerAccountId: "gambler-id"
        )
        .environment(\.diResolver, diFakeResolver())
    }
}
