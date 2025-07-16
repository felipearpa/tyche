import Foundation
import Session

class PoolScoreListDrawerViewModel : ObservableObject {
    private let logOutUseCase: LogOutUseCase

    init(logOutUseCase: LogOutUseCase) {
        self.logOutUseCase = logOutUseCase
    }

    @MainActor
    func logOut() {
        Task {
            await logOutUseCase.execute()
        }
    }
}
