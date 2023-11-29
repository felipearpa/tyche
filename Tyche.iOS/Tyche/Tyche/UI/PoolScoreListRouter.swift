import SwiftUI
import Swinject
import Core
import Session
import DataPool
import Pool

struct PoolScoreListRouter: View {
    let diResolver: DIResolver
    let user: AccountBundle
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
        user: AccountBundle(userId: "userId", username: "username"),
        onPoolSelected: { _ in }
    )
}
