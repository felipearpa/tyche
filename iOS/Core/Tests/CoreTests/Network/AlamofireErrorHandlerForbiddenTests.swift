import Alamofire
import Testing
@testable import Core

@Suite("AlamofireErrorHandler — forbidden mapping")
struct AlamofireErrorHandlerForbiddenTests {

    @Test("given a 403 AFError when handle is called then NetworkError.http with forbidden is returned")
    func forbiddenAFErrorMapsToNetworkErrorHttp() async {
        let handler = AlamofireErrorHandler()

        let result: Result<String, Error> = await handler.handle {
            throw AFError.responseValidationFailed(reason: .unacceptableStatusCode(code: 403))
        }

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            guard let networkError = error as? NetworkError else {
                Issue.record("Expected NetworkError, got \(error)")
                return
            }
            if case .http(let code) = networkError {
                #expect(code == .forbidden)
            } else {
                Issue.record("Expected NetworkError.http, got \(networkError)")
            }
        }
    }

    @Test("given a 500 AFError when handle is called then NetworkError.http with internalServerError is returned")
    func internalServerErrorAFErrorMapsToNetworkErrorHttp() async {
        let handler = AlamofireErrorHandler()

        let result: Result<String, Error> = await handler.handle {
            throw AFError.responseValidationFailed(reason: .unacceptableStatusCode(code: 500))
        }

        switch result {
        case .success:
            Issue.record("Expected failure but got success")
        case .failure(let error):
            guard let networkError = error as? NetworkError else {
                Issue.record("Expected NetworkError, got \(error)")
                return
            }
            if case .http(let code) = networkError {
                #expect(code == .internalServerError)
            } else {
                Issue.record("Expected NetworkError.http, got \(networkError)")
            }
        }
    }
}
