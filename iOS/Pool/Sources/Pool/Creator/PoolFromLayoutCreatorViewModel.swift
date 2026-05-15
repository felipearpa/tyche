import Foundation
import DataPool
import UI
import ViewingState

public class PoolFromLayoutCreatorViewModel : ObservableObject {
    let gamblerId: String

    let createPoolUseCase: CreatePoolUseCase

    @Published @MainActor private(set) var state: MutationState<CreatePoolModel> = .idle(emptyCreatePoolModel())

    public init(gamblerId: String, createPoolUseCase: CreatePoolUseCase) {
        self.gamblerId = gamblerId
        self.createPoolUseCase = createPoolUseCase
    }

    @MainActor
    func createPool(createPoolModel: CreatePoolModel) {
        Task {
            let currentCreatePoolModel = state.activeValue()
            state = .mutating(original: currentCreatePoolModel, updated: createPoolModel)

            let result = await createPoolUseCase.execute(createPoolInput: createPoolModel.toCreatePoolInput(ownerGamblerId: gamblerId))

            switch result {
            case .success(let createdPoolOutput):
                state = .mutated(
                    original: currentCreatePoolModel,
                    updated: createPoolModel.copy { builder in builder.poolId = createdPoolOutput.poolId }
                )
            case .failure(let error):
                state = .failure(original: currentCreatePoolModel, updated: createPoolModel, error: error)
            }
        }
    }

    @MainActor
    func reset() {
        state = .idle(state.activeValue())
    }
}
