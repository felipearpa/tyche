import XCTest
@testable import Core

final class ResultTests: XCTestCase {
    func testGivenASuccessResultWhenIsFoldedThenTheSuccessTransformationIsPerfomed() throws {
        let successResult: Result<String, Error> = .success("Ok")
        let expectedTransformedSuccess = "expectedOk"
        var isSuccessTransformPerformed = false
        var isFailureTransformPerformed = false
        
        let transformedSuccess = successResult.fold(
            onSuccess: { _ in
                isSuccessTransformPerformed = true
                return expectedTransformedSuccess
            },
            onFailure: { _ in
                isFailureTransformPerformed = true
                return "failure"
            }
        )
        
        XCTAssertTrue(isSuccessTransformPerformed)
        XCTAssertFalse(isFailureTransformPerformed)
        XCTAssertEqual(expectedTransformedSuccess, transformedSuccess)
    }
    
    func testGivenAFailureResultWhenIsFoldedThenTheFailureTransformationIsPerfomed() throws {
        class CustomError: Error {}
        
        let failureResult: Result<String, Error> = .failure(CustomError())
        let expectedTransformedFailure = "expecedFailure"
        var isSuccessTransformPerformed = false
        var isFailureTransformPerformed = false
        
        let transformedFailure = failureResult.fold(
            onSuccess: { _ in
                isSuccessTransformPerformed = true
                return "Ok"
            },
            onFailure: { _ in
                isFailureTransformPerformed = true
                return expectedTransformedFailure
            }
        )
        
        XCTAssertFalse(isSuccessTransformPerformed)
        XCTAssertTrue(isFailureTransformPerformed)
        XCTAssertEqual(expectedTransformedFailure, transformedFailure)
    }
}
