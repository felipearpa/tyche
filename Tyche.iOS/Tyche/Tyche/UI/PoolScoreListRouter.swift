import SwiftUI
import Swinject
import Core
import User
import Pool

struct PoolScoreListRouter: View {
    let diResolver: DIResolver
    let user: UserProfile
    let onPoolSelected: (PoolProfile) -> Void
    
    @State private var path = NavigationPath()
    
    var body: some View {
        let _ = Self._printChanges()

        NavigationStack(path: $path) {
            PoolScoreListView(
                viewModel: PoolScoreListViewModel(
                    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    gamblerId: user.userId
                ),
                onPoolDetailRequested: { pool in onPoolSelected(pool) }
            )
        }
    }
}

#Preview {
    PoolScoreListRouter(
        diResolver: DIResolver(resolver:Assembler([]).resolver),
        user: UserProfile(userId: "userId", username: "username"),
        onPoolSelected: { _ in }
    )
}
