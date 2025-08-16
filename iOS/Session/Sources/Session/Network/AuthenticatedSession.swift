import Foundation
import Alamofire

private struct EmptyParameters: Codable {
    public init() {}
}

public class AuthenticatedSession {
    private let session: Session
    
    public init(authTokenRetriever: AuthTokenRetriever) {
        let configuration = URLSessionConfiguration.af.default
        session = Session(
            configuration: configuration,
            interceptor: AuthInterceptor(authTokenRetriever: authTokenRetriever)
        )
    }
    
    public func request(
        _ url: URL,
        method: HTTPMethod = .get,
        encoder: ParameterEncoder = URLEncodedFormParameterEncoder.default,
        headers: HTTPHeaders? = nil
    ) -> DataRequest {
        return session.request(
            url,
            method: method,
            parameters: EmptyParameters(),
            encoder: encoder,
            headers: headers
        )
    }
    
    public func request<Parameters: Encodable>(
        _ url: URL,
        method: HTTPMethod = .get,
        parameters: Parameters,
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
