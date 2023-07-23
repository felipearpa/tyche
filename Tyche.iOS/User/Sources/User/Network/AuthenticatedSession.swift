import Foundation
import Alamofire

public class AuthenticatedSession {
    private let session: Session
    
    public init(loginStorage: LoginStorage) {
        let configuration = URLSessionConfiguration.af.default
        session = Session(
            configuration: configuration,
            interceptor: AuthInterceptor(loginStorage: loginStorage)
        )
    }
    
    public func request<Parameters: Encodable>(
        _ url: URL,
        parameters: Parameters? = nil
    ) -> DataRequest {
        return session.request(url, parameters: parameters)
    }
}
