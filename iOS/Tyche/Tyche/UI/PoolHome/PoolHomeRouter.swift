import SwiftUI
import Swinject
import Core
import Session
import DataPool
import Bet

struct PoolHomeRouter: View {
    let user: AccountBundle
    let pool: PoolProfile
    let onChangePool: () -> Void
    let onSignOut: () -> Void
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            PoolHomeView(
                gamblerId: user.accountId,
                poolId: pool.poolId,
                onChangePool: onChangePool,
                onSignOut: onSignOut,
                onGamblerOpen: { tappedPoolId, tappedGamblerId, tappedGamblerUsername in
                    if tappedGamblerId != user.accountId {
                        path.append(
                            GamblerBetsRoute(
                                poolId: tappedPoolId,
                                gamblerId: tappedGamblerId,
                                gamblerUsername: tappedGamblerUsername
                            )
                        )
                    }
                },
                onMatchOpen: { poolId, matchId, homeTeamName, awayTeamName, matchDateTime, homeTeamScore, awayTeamScore, isLive in
                    path.append(
                        MatchBetListViewRoute(
                            poolId: poolId,
                            matchId: matchId,
                            homeTeamName: homeTeamName,
                            awayTeamName: awayTeamName,
                            matchDateTime: matchDateTime,
                            homeTeamScore: homeTeamScore,
                            awayTeamScore: awayTeamScore,
                            isLive: isLive
                        )
                    )
                }
            )
            .navigationDestination(for: GamblerBetsRoute.self) { route in
                GamblerBetsView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    gamblerUsername: route.gamblerUsername,
                    onMatchOpen: { poolId, matchId, homeTeamName, awayTeamName, matchDateTime, homeTeamScore, awayTeamScore, isLive in
                        path.append(
                            MatchBetListViewRoute(
                                poolId: poolId,
                                matchId: matchId,
                                homeTeamName: homeTeamName,
                                awayTeamName: awayTeamName,
                                matchDateTime: matchDateTime,
                                homeTeamScore: homeTeamScore,
                                awayTeamScore: awayTeamScore,
                                isLive: isLive
                            )
                        )
                    }
                )
            }
            .navigationDestination(for: MatchBetListViewRoute.self) { route in
                MatchBetListView(
                    poolId: route.poolId,
                    matchId: route.matchId,
                    homeTeamName: route.homeTeamName,
                    awayTeamName: route.awayTeamName,
                    matchDateTime: route.matchDateTime,
                    homeTeamScore: route.homeTeamScore,
                    awayTeamScore: route.awayTeamScore,
                    isLive: route.isLive
                )
            }
        }
        .withParentGeometryProxy()
    }
}
