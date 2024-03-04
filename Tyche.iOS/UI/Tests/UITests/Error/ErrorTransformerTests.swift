import XCTest
@testable import UI
@testable import Core

final class ErrorTransformerTests: XCTestCase {
    func testGivenALocalizedErrorWhenTransformationAttemptThenTheSameLocalizedErrorIsReturned() {
        let newLocalizedError = localizedError.orLocalizedError()
        thenTheSameLocalizedErrorIsReturned(actualLocalizedError: newLocalizedError as! LocalizedError)
    }
    
    func testGivenANetworkErrorWhenTransformationAttemptThenIsTransformedToNetworkLocalizedError() {
        [NetworkError.remoteCommunication, 
         NetworkError.http(code: HTTPStatusCode.internalServerError)].forEach { networkError in
            XCTContext.runActivity(
                named: "given a \(networkError) when transformation attempt then is transformed to NetworkLocalizedError"
            ) { _ in
                let newLocalizedError = networkError.orLocalizedError()
                XCTAssertTrue(newLocalizedError is NetworkLocalizedError)
            }
        }
    }
    
    func testGivenANeitherLocalizedNorNetworkErrorWhenTransformationAttemptThenIsTransformedToUnknownLocalizedError() {
        let newLocalizedError = neitherLocalizedNorNetworkError.orLocalizedError()
        XCTAssertTrue(newLocalizedError is UnknownLocalizedError)
    }
    
    func testGivenALocalizedErrorWhenCheckedForLocalizedErrorTypeThenTheSameLocalizedErrorIsReturned() {
        let newLocalizedError = localizedError.localizedErrorOrNil()
        thenTheSameLocalizedErrorIsReturned(actualLocalizedError: newLocalizedError!)
    }
    
    func testGivenANoLocalizedErrorWhenCheckedForLocalizedErrorTypeThenNilIsReturned() {
        let newLocalizedError = noLocalizedError.localizedErrorOrNil()
        XCTAssertNil(newLocalizedError)
    }
    
    private func thenTheSameLocalizedErrorIsReturned(actualLocalizedError: LocalizedError) {
        XCTAssertEqual(localizedError.errorDescription, actualLocalizedError.errorDescription)
        XCTAssertEqual(localizedError.failureReason, actualLocalizedError.failureReason)
        XCTAssertEqual(localizedError.recoverySuggestion, actualLocalizedError.recoverySuggestion)
        XCTAssertEqual(localizedError.helpAnchor, actualLocalizedError.helpAnchor)
    }
}

private struct TestLocalizedError: LocalizedError {
    let errorDescription: String? = "errorDescription"
    let failureReason: String? = "failureReason"
    let recoverySuggestion: String? = "recoverySuggestion"
    var helpAnchor: String? = "helpAnchor"
}

private struct TestError: Error {
    let message: String = "message"
}

private let localizedError = TestLocalizedError()
private let neitherLocalizedNorNetworkError = TestError()
private let noLocalizedError = TestError()
