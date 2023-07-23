import XCTest
@testable import UI

private struct CustomError : Error {}

final class ViewStateTests: XCTestCase {
    func testGivenALoadingViewStateWhenIsLoadingIsPerformedThenTrueIsReturned() throws {
        let viewState = ViewState<String>.loading
        
        let isLoading = viewState.isLoading()
        
        XCTAssertTrue(isLoading)
    }
    
    func testGivenANotLoadingViewStateWhenIsLoadingIsPerformedThenFalseIsReturned() throws {
        try notLoadingViewStates().forEach { viewState in
            try givenANotLoadingViewStateWhenIsLoadingIsPerformedThenFalseIsReturned(viewState: viewState)
        }
    }
    
    func givenANotLoadingViewStateWhenIsLoadingIsPerformedThenFalseIsReturned(viewState: ViewState<String>) throws {
        XCTAssertFalse(viewState.isLoading())
    }
    
    func testGivenAFailureViewStateWhenIsFailureIsPerformedThenTrueIsReturned() throws {
        let viewState = ViewState<String>.failure(CustomError())
        
        let isFailure = viewState.isFailure()
        
        XCTAssertTrue(isFailure)
    }
    
    func testGivenANotFailureViewStateWhenIsFailureIsPerformedThenFalseIsReturned() throws {
        try notFailureViewStates().forEach { viewState in
            try givenANotFailureViewStateWhenIsFailureIsPerformedThenFalseIsReturned(viewState: viewState)
        }
    }
    
    func givenANotFailureViewStateWhenIsFailureIsPerformedThenFalseIsReturned(viewState: ViewState<String>) throws {
        XCTAssertFalse(viewState.isFailure())
    }
    
    func testGivenAFailureViewStateWhenErrorOrNullIsPerformedThenTheErrorIsReturned() throws {
        let viewState = ViewState<String>.failure(CustomError())
        
        let error = viewState.errorOrNull()
        
        XCTAssertTrue(error is CustomError)
    }
    
    func testGivenANotFailureViewStateWhenErrorOrNullIsPerformedThenNullIsReturned() throws {
        try notFailureViewStates().forEach { viewState in
            try givenANotFailureViewStateWhenErrorOrNullIsPerformedThenNullIsReturned(viewState: viewState)
        }
    }
    
    func givenANotFailureViewStateWhenErrorOrNullIsPerformedThenNullIsReturned(viewState: ViewState<String>) throws {
        let error = viewState.errorOrNull()
        XCTAssertTrue(error == nil)
    }
    
    private func notLoadingViewStates() -> [ViewState<String>] {
        return [ ViewState<String>.initial,
                 ViewState<String>.success("Ok"),
                 ViewState<String>.failure(CustomError()) ]
    }
    
    private func notFailureViewStates() -> [ViewState<String>] {
        return [ ViewState<String>.initial,
                 ViewState<String>.loading,
                 ViewState<String>.success("Ok") ]
    }
}
