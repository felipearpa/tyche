import SwiftUI
import Swinject
import Core
import Session
import DataPool

struct PoolHomeRouter: View {
    let user: AccountBundle
    let pool: PoolProfile
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            PoolHomeView(
                gamblerId: user.accountId,
                poolId: pool.poolId
            )
        }
    }
}
