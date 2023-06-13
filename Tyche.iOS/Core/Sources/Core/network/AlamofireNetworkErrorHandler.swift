import Alamofire

public class AlamofireNetworkErrorHandler : NetworkErrorHandler {
    
    public func handle<T>(_ perform: () async throws -> T) async -> Result<T, Error> {
        do {
            return await .success(try perform())
        } catch let error as AFError {
            if case .responseValidationFailed(.unacceptableStatusCode(let code)) = error {
                guard let httpStatusCode = HTTPStatusCode(rawValue: code) else {
                    return .failure(NetworkError.remoteCommunication)
                }
                return .failure(NetworkError.http(code: httpStatusCode))
            } else {
                return .failure(NetworkError.remoteCommunication)
            }
        } catch {
            return .failure(error)
        }
    }
}
