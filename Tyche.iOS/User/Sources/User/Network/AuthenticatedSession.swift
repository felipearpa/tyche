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
        method: HTTPMethod = .get,
        parameters: Parameters? = nil,
        encoder: ParameterEncoder = URLEncodedFormParameterEncoder.default,
        headers: HTTPHeaders? = nil
    ) -> DataRequest {
        return session.request(
            url,
            method: method,
            parameters: parameters,
            encoder: encoder,
            headers: headers
        )
    }
}
