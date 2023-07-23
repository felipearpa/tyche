import Alamofire

public class AlamofireErrorHandler : NetworkErrorHandler {
    public func handle<Value>(_ perform: () async throws -> Value) async -> Result<Value, Error> {
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
