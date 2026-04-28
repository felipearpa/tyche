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
                }
            )
            .navigationDestination(for: GamblerBetsRoute.self) { route in
                GamblerBetsView(
                    poolId: route.poolId,
                    gamblerId: route.gamblerId,
                    gamblerUsername: route.gamblerUsername
                )
            }
        }
        .withParentGeometryProxy()
    }
}
