import SwiftUI
import Swinject
import Core
import Session
import DataPool
import Pool

struct PoolScoreListRouter: View {
    let user: AccountBundle
    let onPoolSelected: (PoolProfile) -> Void
    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var path = NavigationPath()
    
    var body: some View {
        let _ = Self._printChanges()

        NavigationStack(path: $path) {
            PoolScoreListView(
                viewModel: PoolScoreListViewModel(
                    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase(
                        poolGamblerScoreRepository: diResolver.resolve(PoolGamblerScoreRepository.self)!
                    ),
                    gamblerId: user.accountId
                ),
                onPoolDetailRequested: { pool in onPoolSelected(pool) }
            )
        }
    }
}
