import ViewingState
import Pool

struct PoolHomeDrawerUiState {
    let accountId: String
    let email: String
    let username: String
    let poolGamblerScoreState: LoadState<PoolGamblerScoreModel>
    let isOwner: Bool
    let gamblerCount: Int?
    let isDeleting: Bool
}
