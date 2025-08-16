import XCTest
@testable import UI

private struct CustomError : Error {}

final class LoadableViewStateTests: XCTestCase {
    func testGivenAStateOfInitialWhenCheckedIfInitialThenInitialIsConfirmed() {
        let viewState = LoadableViewState<Void>.initial
        let isInitial = viewState.isInitial()
        XCTAssertTrue(isInitial)
    }
    
    func testGivenAStateOfNoInitialWhenCheckedIfInitialThenInitialIsNotConfirmed() {
        noInitialViewStates().forEach { viewState in
            XCTContext.runActivity(
                named: "given a state of \(viewState) when checked if initial then initial is not confirmed"
            ) { _ in
                let isInitial = viewState.isInitial()
                XCTAssertFalse(isInitial)
            }
        }
    }
    
    func testGivenAStateOfLoadingWhenCheckedIfLoadingThenLoadingIsConfirmed() {
        let viewState = LoadableViewState<Void>.loading
        let isLoading = viewState.isLoading()
        XCTAssertTrue(isLoading)
    }
    
    func testGivenAStateOfNoLoadingWhenCheckedIfLoadingThenLoadingIsNotConfirmed() {
        noLoadingViewStates().forEach { viewState in
            XCTContext.runActivity(
                named: "given a state of \(viewState) when checked if loading then loading is not confirmed"
            ) { _ in
                let isLoading = viewState.isLoading()
                XCTAssertFalse(isLoading)
            }
        }
    }
    
    func testGivenAStateOfFailureWhenCheckedIfFailureThenFailureIsConfirmed() {
        let viewState = LoadableViewState<Void>.failure(CustomError())
        let isFailure = viewState.isFailure()
        XCTAssertTrue(isFailure)
    }
    
    func testGivenAStateOfNoFailureWhenCheckedIfFailureThenFailureIsConfirmed() {
        noFailureViewStates().forEach { viewState in
            XCTContext.runActivity(
                named: "given a state of \(viewState) when checked if failure then failure is confirmed"
            ) { _ in
                let isFailure = viewState.isFailure()
                XCTAssertFalse(isFailure)
            }
        }
    }
    
    func testGivenAStateOfSuccessWhenCheckedIfSuccessThenSuccessIsConfirmed() {
        let viewState = LoadableViewState<Void>.success(())
        let isSuccess = viewState.isSuccess()
        XCTAssertTrue(isSuccess)
    }
    
    func testGivenAStateOfNoSuccessWhenCheckedIfSuccessThenSuccessIsConfirmed() {
        noSuccessViewStates().forEach { viewState in
            XCTContext.runActivity(
                named: "given a state of \(viewState) when checked if success then success is confirmed"
            ) { _ in
                let isSuccess = viewState.isSuccess()
                XCTAssertFalse(isSuccess)
            }
        }
    }
    
    func testGivenAFailureWhenCheckedForErrosThenTheErrorIsFound() {
        let viewState = LoadableViewState<String>.failure(CustomError())
        let error = viewState.errorOrNil()
        XCTAssertTrue(error is CustomError)
    }
    
    func testGivenANoFailureWhenCheckedForErrosThenTheErrorIsNotFound() {
        noFailureViewStates().forEach { viewState in
            let error = viewState.errorOrNil()
            XCTAssertTrue(error == nil)
        }
    }
    
    private func noInitialViewStates() -> [LoadableViewState<Void>] {
        return [ LoadableViewState<Void>.loading,
                 LoadableViewState<Void>.success(()),
                 LoadableViewState<Void>.failure(CustomError()) ]
    }
    
    private func noLoadingViewStates() -> [LoadableViewState<Void>] {
        return [ LoadableViewState<Void>.initial,
                 LoadableViewState<Void>.success(()),
                 LoadableViewState<Void>.failure(CustomError()) ]
    }
    
    private func noFailureViewStates() -> [LoadableViewState<Void>] {
        return [ LoadableViewState<Void>.initial,
                 LoadableViewState<Void>.loading,
                 LoadableViewState<Void>.success(()) ]
    }
    
    private func noSuccessViewStates() -> [LoadableViewState<Void>] {
        return [ LoadableViewState<Void>.initial,
                 LoadableViewState<Void>.loading,
                 LoadableViewState<Void>.failure(CustomError()) ]
    }
}

