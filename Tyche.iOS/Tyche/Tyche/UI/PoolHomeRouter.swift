import SwiftUI
import Swinject
import Core
import User
import Pool

struct PoolHomeRouter: View {
    let diResolver: DIResolver
    let user: UserProfile
    let pool: PoolProfile
    
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            PoolHomeView(
                diResolver: diResolver,
                gamblerId: user.userId,
                poolId: pool.poolId
            )
        }
    }
}

#Preview {
    PoolHomeRouter(
        diResolver: DIResolver(resolver:Assembler([]).resolver),
        user: UserProfile(userId: "userId", username: "username"),
        pool: PoolProfile(poolId: "poolId")
    )
}
