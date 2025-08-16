import Foundation
import Alamofire

class AuthInterceptor: RequestInterceptor {
    private let authTokenRetriever: AuthTokenRetriever
    
    init(authTokenRetriever: AuthTokenRetriever) {
        self.authTokenRetriever = authTokenRetriever
    }
    
    func adapt(
        _ urlRequest: URLRequest,
        for session: Session,
        completion: @escaping (Result<URLRequest, Error>) -> Void
    ) {
        Task {
            guard let token = await authTokenRetriever.authToken() else {
                completion(.success(urlRequest))
                return
            }
            
            var newUrlRequest = urlRequest
            newUrlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            completion(.success(newUrlRequest))
        }
    }
}
