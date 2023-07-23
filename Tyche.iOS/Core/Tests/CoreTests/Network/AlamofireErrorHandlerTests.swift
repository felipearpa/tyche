import XCTest
@testable import Core

import Alamofire

private let SUCCESS_VALUE = "Ok"
private let ERROR_HTTP_CODE = 500

final class AlamofireNetworkErrorHandlerTests: XCTestCase {
    private var networkErrorHandler = AlamofireErrorHandler()
    
    func testGivenASuccessValueWhenTheExecutionIsHandledThenASuccessResultIsReturned() async throws {
        let successPerform = successPerform()
        
        let result = await networkErrorHandler.handle(successPerform)
        
        assertPerformIsSuccess(result)
    }
    
    private func successPerform() -> () async throws -> String {
        let perform: () async throws -> String = {
            return SUCCESS_VALUE
        }
        return perform
    }
    
    private func assertPerformIsSuccess(_ result: Result<String, Error>) {
        switch result {
        case .success(let value):
            XCTAssertEqual(SUCCESS_VALUE, value)
        case .failure:
            XCTFail("Expected a success result, but failure received")
        }
    }
    
    func testGivenAUnacceptableStatusCodeFailureWhenTheExcutionIsHandledThenAFailureResultIsReturned() async throws {
        let failurePerform = unacceptableStatusCodeFailurePerform()
        
        let result = await networkErrorHandler.handle(failurePerform)
        
        assertPerformIsHttpFailure(result)
    }
    
    private func unacceptableStatusCodeFailurePerform() -> () async throws -> String {
        let failurePerform: () async throws -> String = {
            throw AFError.responseValidationFailed(
                reason: AFError.ResponseValidationFailureReason.unacceptableStatusCode(code: ERROR_HTTP_CODE)
            )
        }
        return failurePerform
    }
    
    private func assertPerformIsHttpFailure(_ result: Result<String, Error>) {
        switch result {
        case .success:
            XCTFail("Expected a failure result, but success received")
        case .failure(let error):
            XCTAssertTrue(error is NetworkError)
            
            let networkError = error as! NetworkError
            switch networkError {
            case .http(let code):
                XCTAssertEqual(ERROR_HTTP_CODE, code.rawValue)
            default:
                XCTFail("Expected httpFailure, but other received")
            }
        }
    }
    
    func testGivenAResponseSerializationFailureWhenTheExcutionIsHandledThenAFailureResultIsReturned() async throws {
        let failurePerform = responseSerializationFailurePerform()
        
        let result = await networkErrorHandler.handle(failurePerform)
        
        assertPerformIsRemoteCommunicationFailure(result)
    }
    
    private func responseSerializationFailurePerform() -> () async throws -> String {
        let failurePerform: () async throws -> String = {
            throw AFError.responseSerializationFailed(reason: AFError.ResponseSerializationFailureReason.inputDataNilOrZeroLength)
        }
        return failurePerform
    }
    
    private func assertPerformIsRemoteCommunicationFailure(_ result: Result<String, Error>) {
        switch result {
        case .success:
            XCTFail("Expected a failure result, but success received")
        case .failure(let error):
            XCTAssertTrue(error is NetworkError)
            
            let networkError = error as! NetworkError
            switch networkError {
            case .remoteCommunication:
                XCTAssertTrue(true)
            default:
                XCTFail("Expected remoteCommunicationFailure, but other received")
            }
        }
    }
    
    func testGivenANotAlmofireFailureWhenTheExcutionIsHandledThenAFailureResultIsReturned() async throws {
        let failurePerform = notAlmofireFailurePerform()
        
        let result = await networkErrorHandler.handle(failurePerform)
        
        assertPerformIsNotAlmofireFailure(result)
    }
    
    private func notAlmofireFailurePerform() -> () async throws -> String {
        struct CustomError: Error {}

        let failurePerform: () async throws -> String = {
            throw CustomError()
        }
        
        return failurePerform
    }
    
    private func assertPerformIsNotAlmofireFailure(_ result: Result<String, Error>) {
        switch result {
        case .success:
            XCTFail("Expected a failure result, but success received")
        case .failure(let error):
            XCTAssertFalse(error is AFError)
        }
    }
}
