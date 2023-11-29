import Core

class LoginRemoteRepository : LoginRepository {
    private let loginRemoteDataSource: LoginRemoteDataSource
    private let networkErrorHandler: NetworkErrorHandler
    
    init(
        loginRemoteDataSource: LoginRemoteDataSource,
        networkErrorHandler: NetworkErrorHandler
    ) {
        self.loginRemoteDataSource = loginRemoteDataSource
        self.networkErrorHandler = networkErrorHandler
    }
    
    func login(loginCredential: LoginCredential) async -> Result<LoginBundle, Error> {
        return await networkErrorHandler.handle {
            let loginResponse = try await loginRemoteDataSource.login(
                loginRequest: loginCredential.toRequest()
            )
            return loginResponse.toLoginBundle()
        }.mapNetworkError { networkError in
            if case .http(let code) = networkError {
                if code == .unauthorized {
                    return LoginError.InvalidCredentials
                }
            }
            return networkError
        }
    }
}
