import Foundation
import Alamofire

/// Attaches the signed-in user's ID token to each request and, when a request gets a 401, forces one
/// token refresh and retries that request once.
///
/// When the forced refresh fails or yields no token, the request is not retried and the original 401
/// surfaces.
final class AuthInterceptor: RequestInterceptor {
    private let authTokenRetriever: AuthTokenRetriever
    private let forcedTokenRefresher: ForcedTokenRefresher
    
    init(authTokenRetriever: AuthTokenRetriever) {
        self.authTokenRetriever = authTokenRetriever
        self.forcedTokenRefresher = ForcedTokenRefresher(authTokenRetriever: authTokenRetriever)
    }
    
    func adapt(
        _ urlRequest: URLRequest,
        for session: Session,
        completion: @escaping @Sendable (Result<URLRequest, any Error>) -> Void
    ) {
        let authTokenRetriever = authTokenRetriever
        Task {
            do {
                guard let token = try await authTokenRetriever.authToken(forceRefresh: false) else {
                    completion(.success(urlRequest))
                    return
                }
                
                var newUrlRequest = urlRequest
                newUrlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                completion(.success(newUrlRequest))
            } catch {
                completion(.failure(error))
            }
        }
    }
    
    func retry(
        _ request: Request,
        for session: Session,
        dueTo error: any Error,
        completion: @escaping @Sendable (RetryResult) -> Void
    ) {
        guard request.retryCount == 0, request.response?.statusCode == 401 else {
            completion(.doNotRetry)
            return
        }
        
        let forcedTokenRefresher = forcedTokenRefresher
        Task {
            let refreshedToken = try? await forcedTokenRefresher.refresh()
            completion(refreshedToken == nil ? .doNotRetry : .retry)
        }
    }
}
