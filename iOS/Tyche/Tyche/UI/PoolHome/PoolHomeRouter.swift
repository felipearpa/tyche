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
                            BetTimelineListViewRoute(
                                poolId: tappedPoolId,
                                gamblerId: tappedGamblerId,
                                gamblerUsername: tappedGamblerUsername
                            )
                        )
                    }
                },
                onMatchOpen: { poolId, gamblerId, matchId in
                    path.append(
                        MatchBetListViewRoute(
                            poolId: poolId,
                            gamblerId: gamblerId,
                            matchId: matchId
                        )
                    )
                }
            )
            .navigationDestination(for: BetTimelineListViewRoute.self) { route in
                BetTimelineListView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    gamblerUsername: route.gamblerUsername,
                    onMatchOpen: { poolId, gamblerId, matchId in
                        path.append(
                            MatchBetListViewRoute(
                                poolId: poolId,
                                gamblerId: gamblerId,
                                matchId: matchId
                            )
                        )
                    }
                )
            }
            .navigationDestination(for: MatchBetListViewRoute.self) { route in
                MatchBetListView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    matchId: route.matchId,
                    onGamblerOpen: { tappedPoolId, tappedGamblerId, tappedGamblerUsername in
                        if tappedGamblerId != user.accountId {
                            path.append(
                                BetTimelineListViewRoute(
                                    poolId: tappedPoolId,
                                    gamblerId: tappedGamblerId,
                                    gamblerUsername: tappedGamblerUsername
                                )
                            )
                        }
                    }
                )
            }
        }
        .withParentGeometryProxy()
    }
}
