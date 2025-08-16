import Foundation
import DataPool
import UI

public class PoolFromLayoutCreatorViewModel : ObservableObject {
    let gamblerId: String

    let createPoolUseCase: CreatePoolUseCase

    @Published @MainActor private(set) var state: EditableViewState<CreatePoolModel> = .initial(emptyCreatePoolModel())

    public init(gamblerId: String, createPoolUseCase: CreatePoolUseCase) {
        self.gamblerId = gamblerId
        self.createPoolUseCase = createPoolUseCase
    }

    @MainActor
    func createPool(createPoolModel: CreatePoolModel) {
        Task {
            let currentCreatePoolModel = state.relevantValue()
            state = .saving(current: currentCreatePoolModel, target: createPoolModel)

            let result = await createPoolUseCase.execute(createPoolInput: createPoolModel.toCreatePoolInput(ownerGamblerId: gamblerId))

            switch result {
            case .success(let createdPoolOutput):
                state = .success(
                    old: currentCreatePoolModel,
                    succeeded: createPoolModel.copy { builder in builder.poolId = createdPoolOutput.poolId }
                )
            case .failure(let error):
                state = .failure(current: currentCreatePoolModel, failed: createPoolModel, error: error)
            }
        }
    }

    @MainActor
    func reset() {
        state = .initial(state.relevantValue())
    }
}
