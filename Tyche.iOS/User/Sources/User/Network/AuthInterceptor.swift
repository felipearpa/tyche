import Foundation
import Alamofire

class AuthInterceptor : RequestInterceptor {
    private let loginStorage: LoginStorage
    
    init(loginStorage: LoginStorage) {
        self.loginStorage = loginStorage
    }
    
    func adapt(
        _ urlRequest: URLRequest,
        for session: Session,
        completion: @escaping (Result<URLRequest, Error>) -> Void
    ) {
        guard let loginProfile = try? loginStorage.get() else {
            completion(.success(urlRequest))
            return
        }
        
        var newUrlRequest = urlRequest
        newUrlRequest.setValue("Bearer \(loginProfile.token)", forHTTPHeaderField: "Authorization")
        completion(.success(newUrlRequest))
    }
}
