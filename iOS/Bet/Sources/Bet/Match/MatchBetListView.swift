import SwiftUI
import Core
import DataBet

public struct MatchBetListView: View {
    let poolId: String
    let matchId: String
    let homeTeamName: String
    let awayTeamName: String
    let matchDateTime: Date
    let homeTeamScore: Int?
    let awayTeamScore: Int?
    let isLive: Bool

    @Environment(\.diResolver) var diResolver: DIResolver
    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        poolId: String,
        matchId: String,
        homeTeamName: String,
        awayTeamName: String,
        matchDateTime: Date,
        homeTeamScore: Int?,
        awayTeamScore: Int?,
        isLive: Bool = false
    ) {
        self.poolId = poolId
        self.matchId = matchId
        self.homeTeamName = homeTeamName
        self.awayTeamName = awayTeamName
        self.matchDateTime = matchDateTime
        self.homeTeamScore = homeTeamScore
        self.awayTeamScore = awayTeamScore
        self.isLive = isLive
    }

    public var body: some View {
        let _ = Self._printChangesIfDebug()

        VStack(spacing: boxSpacing.medium) {
            MatchHeader(
                homeTeamName: homeTeamName,
                awayTeamName: awayTeamName,
                matchDateTime: matchDateTime,
                homeTeamScore: homeTeamScore,
                awayTeamScore: awayTeamScore,
                isLive: isLive
            )

            MatchBetListContent(
                viewModel: MatchBetListViewModel(
                    getPoolMatchGamblerBetsUseCase: GetPoolMatchGamblerBetsUseCase(
                        poolGamblerBetRepository: diResolver.resolve(PoolGamblerBetRepository.self)!
                    ),
                    poolId: poolId,
                    matchId: matchId
                )
            )
        }
        .padding(boxSpacing.medium)
        .navigationTitle(String(.matchBetsViewTitle))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct MatchBetListContent: View {
    @StateObject private var viewModel: MatchBetListViewModel

    init(viewModel: @autoclosure @escaping () -> MatchBetListViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        MatchBetList(lazyPagingItems: viewModel.lazyPager)
            .refreshable { viewModel.refresh() }
            .onAppearOnce { viewModel.refresh() }
    }
}

private struct MatchHeader: View {
    let homeTeamName: String
    let awayTeamName: String
    let matchDateTime: Date
    let homeTeamScore: Int?
    let awayTeamScore: Int?
    let isLive: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(alignment: .center, spacing: boxSpacing.medium) {
            if isLive {
                LiveIndicator()
            }

            HStack(spacing: boxSpacing.medium) {
                Text(homeTeamName)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity)

                if !isLive {
                    Text(homeTeamScore.map { String($0) } ?? "")
                        .font(.title2)

                    Text("-")

                    Text(awayTeamScore.map { String($0) } ?? "")
                        .font(.title2)
                }

                Text(awayTeamName)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity)
            }

            Text(matchDateTime.toShortDateTimeString())
                .font(.caption)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct LiveIndicator: View {
    @State private var isPulsing = false

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.small) {
            Circle()
                .fill(Color.accentColor)
                .frame(width: 8, height: 8)
                .opacity(isPulsing ? 0.3 : 1.0)
                .animation(
                    .easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                    value: isPulsing
                )

            Text(String(.liveLabel))
                .font(.caption2)
                .foregroundStyle(Color.accentColor)
        }
        .onAppear { isPulsing = true }
    }
}

#Preview("Live") {
    NavigationStack {
        MatchBetListView(
            poolId: "pool-id",
            matchId: "match-id",
            homeTeamName: "Home FC",
            awayTeamName: "Away United",
            matchDateTime: Date(),
            homeTeamScore: 2,
            awayTeamScore: 1,
            isLive: true
        )
        .environment(\.diResolver, diFakeResolver())
    }
}

#Preview("Not live") {
    NavigationStack {
        MatchBetListView(
            poolId: "pool-id",
            matchId: "match-id",
            homeTeamName: "Home FC",
            awayTeamName: "Away United",
            matchDateTime: Date(),
            homeTeamScore: 2,
            awayTeamScore: 1,
            isLive: false
        )
        .environment(\.diResolver, diFakeResolver())
    }
}
