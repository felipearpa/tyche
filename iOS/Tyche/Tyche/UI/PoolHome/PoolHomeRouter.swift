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
                loggedInGamblerId: user.accountId,
                poolId: pool.poolId,
                onChangePool: onChangePool,
                onSignOut: onSignOut,
                onGamblerOpen: { tappedPoolId, tappedGamblerId in
                    if tappedGamblerId != user.accountId {
                        path.append(PoolHomeRoute(poolId: tappedPoolId, gamblerId: tappedGamblerId))
                    }
                }
            )
            .navigationDestination(for: PoolHomeRoute.self) { route in
                PoolHomeView(
                    gamblerId: route.gamblerId,
                    loggedInGamblerId: user.accountId,
                    poolId: route.poolId,
                    onChangePool: onChangePool,
                    onSignOut: onSignOut,
                    onGamblerOpen: { tappedPoolId, tappedGamblerId in
                        if tappedGamblerId != route.gamblerId {
                            path.append(PoolHomeRoute(poolId: tappedPoolId, gamblerId: tappedGamblerId))
                        }
                    }
                )
            }
        }
        .withParentGeometryProxy()
    }
}
