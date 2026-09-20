public class LogOutUseCase {
    private let authenticationRepository: AuthenticationRepository
    private let currentAccountCoordinator: CurrentAccountCoordinator

    init(
        authenticationRepository: AuthenticationRepository,
        currentAccountCoordinator: CurrentAccountCoordinator
    ) {
        self.authenticationRepository = authenticationRepository
        self.currentAccountCoordinator = currentAccountCoordinator
    }

    public func execute() async -> Result<Void, Error> {
        let logoutResult = await authenticationRepository.logOut()
        if case .success = logoutResult {
            do {
                try await currentAccountCoordinator.clear()
            } catch {
                return .failure(error)
            }
        }
        return logoutResult
    }
}
