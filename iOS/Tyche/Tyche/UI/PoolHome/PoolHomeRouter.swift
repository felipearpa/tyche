import SwiftUI
import Swinject
import Core
import Session
import DataPool

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
                onMatchOpen: { poolId, matchId, homeTeamName, awayTeamName, matchDateTime, homeTeamScore, awayTeamScore in
                    path.append(
                        MatchBetsRoute(
                            poolId: poolId,
                            matchId: matchId,
                            homeTeamName: homeTeamName,
                            awayTeamName: awayTeamName,
                            matchDateTime: matchDateTime,
                            homeTeamScore: homeTeamScore,
                            awayTeamScore: awayTeamScore
                        )
                    )
                }
            )
            .navigationDestination(for: GamblerBetsRoute.self) { route in
                GamblerBetsView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    gamblerUsername: route.gamblerUsername,
                    onMatchOpen: { poolId, matchId, homeTeamName, awayTeamName, matchDateTime, homeTeamScore, awayTeamScore in
                        path.append(
                            MatchBetsRoute(
                                poolId: poolId,
                                matchId: matchId,
                                homeTeamName: homeTeamName,
                                awayTeamName: awayTeamName,
                                matchDateTime: matchDateTime,
                                homeTeamScore: homeTeamScore,
                                awayTeamScore: awayTeamScore
                            )
                        )
                    }
                )
            }
            .navigationDestination(for: MatchBetsRoute.self) { route in
                MatchBetsView(
                    poolId: route.poolId,
                    matchId: route.matchId,
                    homeTeamName: route.homeTeamName,
                    awayTeamName: route.awayTeamName,
                    matchDateTime: route.matchDateTime,
                    homeTeamScore: route.homeTeamScore,
                    awayTeamScore: route.awayTeamScore
                )
            }
        }
        .withParentGeometryProxy()
    }
}
