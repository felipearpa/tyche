import Foundation
import Alamofire

class AuthInterceptor: RequestInterceptor {
    private let authStorage: AuthStorage
    
    init(authStorage: AuthStorage) {
        self.authStorage = authStorage
    }
    
    func adapt(
        _ urlRequest: URLRequest,
        for session: Session,
        completion: @escaping (Result<URLRequest, Error>) -> Void
    ) {
        Task {
            guard let loginProfile = try? await authStorage.retrieve() else {
                completion(.success(urlRequest))
                return
            }
            
            var newUrlRequest = urlRequest
            newUrlRequest.setValue("Bearer \(loginProfile.token)", forHTTPHeaderField: "Authorization")
            completion(.success(newUrlRequest))
        }
    }
}
