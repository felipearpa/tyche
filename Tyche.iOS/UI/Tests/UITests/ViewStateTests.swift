import XCTest
@testable import UI

private struct CustomError : Error {}

final class ViewStateTests: XCTestCase {
    func testGivenALoadingViewStateWhenIsLoadingIsPerformedThenTrueIsReturned() throws {
        let viewState = LodableViewState<String>.loading
        
        let isLoading = viewState.isLoading()
        
        XCTAssertTrue(isLoading)
    }
    
    func testGivenANotLoadingViewStateWhenIsLoadingIsPerformedThenFalseIsReturned() throws {
        try notLoadingViewStates().forEach { viewState in
            try givenANotLoadingViewStateWhenIsLoadingIsPerformedThenFalseIsReturned(viewState: viewState)
        }
    }
    
    func givenANotLoadingViewStateWhenIsLoadingIsPerformedThenFalseIsReturned(viewState: LodableViewState<String>) throws {
        XCTAssertFalse(viewState.isLoading())
    }
    
    func testGivenAFailureViewStateWhenIsFailureIsPerformedThenTrueIsReturned() throws {
        let viewState = LodableViewState<String>.failure(CustomError())
        
        let isFailure = viewState.isFailure()
        
        XCTAssertTrue(isFailure)
    }
    
    func testGivenANotFailureViewStateWhenIsFailureIsPerformedThenFalseIsReturned() throws {
        try notFailureViewStates().forEach { viewState in
            try givenANotFailureViewStateWhenIsFailureIsPerformedThenFalseIsReturned(viewState: viewState)
        }
    }
    
    func givenANotFailureViewStateWhenIsFailureIsPerformedThenFalseIsReturned(viewState: LodableViewState<String>) throws {
        XCTAssertFalse(viewState.isFailure())
    }
    
    func testGivenAFailureViewStateWhenErrorOrNullIsPerformedThenTheErrorIsReturned() throws {
        let viewState = LodableViewState<String>.failure(CustomError())
        
        let error = viewState.errorOrNull()
        
        XCTAssertTrue(error is CustomError)
    }
    
    func testGivenANotFailureViewStateWhenErrorOrNullIsPerformedThenNullIsReturned() throws {
        try notFailureViewStates().forEach { viewState in
            try givenANotFailureViewStateWhenErrorOrNullIsPerformedThenNullIsReturned(viewState: viewState)
        }
    }
    
    func givenANotFailureViewStateWhenErrorOrNullIsPerformedThenNullIsReturned(viewState: LodableViewState<String>) throws {
        let error = viewState.errorOrNull()
        XCTAssertTrue(error == nil)
    }
    
    private func notLoadingViewStates() -> [LodableViewState<String>] {
        return [ LodableViewState<String>.initial,
                 LodableViewState<String>.success("Ok"),
                 LodableViewState<String>.failure(CustomError()) ]
    }
    
    private func notFailureViewStates() -> [LodableViewState<String>] {
        return [ LodableViewState<String>.initial,
                 LodableViewState<String>.loading,
                 LodableViewState<String>.success("Ok") ]
    }
}
