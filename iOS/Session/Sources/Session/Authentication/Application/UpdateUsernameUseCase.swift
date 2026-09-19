import Foundation

public class UpdateUsernameUseCase {
    private let currentAccountCoordinator: CurrentAccountCoordinator

    init(currentAccountCoordinator: CurrentAccountCoordinator) {
        self.currentAccountCoordinator = currentAccountCoordinator
    }

    public func execute(username: String) async -> Result<String, Error> {
        await currentAccountCoordinator.updateUsername(username)
    }
}

public enum UpdateUsernameError: Error {
    case noStoredAccount
}
