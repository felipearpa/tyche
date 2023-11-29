import Alamofire
import Core

class LoginAlamofireDataSource: LoginRemoteDataSource {
    private let urlBasePathProvider: URLBasePathProvider
    
    init(urlBasePathProvider: URLBasePathProvider) {
        self.urlBasePathProvider = urlBasePathProvider
    }
    
    func login(loginRequest: LoginRequest) async throws -> LoginResponse {
        do {
            return try await withUnsafeThrowingContinuation { continuation in
                AF.request(
                    urlBasePathProvider.prependBasePath("user/login")!,
                    method: .post,
                    parameters: loginRequest,
                    encoder: JSONParameterEncoder.default).responseDecodable(of: LoginResponse.self) { response in
                        
                        switch response.result {
                        case .success(let data):
                            continuation.resume(returning: data)
                        case .failure(let error):
                            continuation.resume(throwing: error)
                        }
                    }
                    .validate()
            }
        } catch {
            throw error
        }
    }
}
