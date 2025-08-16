import XCTest
@testable import Core

private struct RuntimeError: Error {}

private let transformedError: Error = RuntimeError()

private let nonNetworkFailureResult: Result<String, Error> = .failure(RuntimeError())

private class NetworkErrorTransformer {
    var wasTransformed = false
    
    func transform(networkError: NetworkError) -> Error {
        wasTransformed = true
        return transformedError
    }
}

final class NetworkErrorTests: XCTestCase {
    func testGivenANetworkExceptionFailureResultAndATransformFunctionWhenANetworkExceptionIsRecoverThenATransformedFailureResultIsReturned() {
        let suite: [Result<String, Error>] = [
            .failure(NetworkError.remoteCommunication),
            .failure(NetworkError.http(code: HTTPStatusCode.internalServerError))
        ]
        suite.forEach { networkFailureResult in
            XCTContext.runActivity(
                named: "given \(networkFailureResult) and a transform function when a NetworkException is recover then a transformed failure result is returned"
            ) { _ in
                let networkErrorTransformer = NetworkErrorTransformer()
                let actualResult = networkFailureResult.recoverNetworkError(networkErrorTransformer.transform)
                verifyTransformedResultWasApplied(
                    networkErrorTransformer: networkErrorTransformer,
                    actualResult: actualResult
                )
            }
        }
    }
    
    func testGivenANonNetworkExceptionFailureResultAndATransformFunctionWhenANetworkExceptionIsRecoverThenTheSourceResultIsReturned() {
        let networkErrorTransformer = NetworkErrorTransformer()
        let actualResult = nonNetworkFailureResult.recoverNetworkError(networkErrorTransformer.transform)
        verifyTransformedResultWasNotApplied(
            networkErrorTransformer: networkErrorTransformer,
            actualResult: actualResult
        )
    }
    
    private func verifyTransformedResultWasApplied(
        networkErrorTransformer: NetworkErrorTransformer,
        actualResult: Result<String, Error>
    ) {
        XCTAssertTrue(actualResult.isFailure)
        
        let actualTransformedException = actualResult.errorOrNil()!
        XCTAssertTrue(networkErrorTransformer.wasTransformed)
        XCTAssertTrue(actualTransformedException is RuntimeError)
    }
    
    private func verifyTransformedResultWasNotApplied(
        networkErrorTransformer: NetworkErrorTransformer,
        actualResult: Result<String, Error>
    ) {
        XCTAssertFalse(networkErrorTransformer.wasTransformed)
        
        let actualError = actualResult.errorOrNil()!
        XCTAssertTrue(actualError is RuntimeError)
    }
}
