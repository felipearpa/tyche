import XCTest
@testable import UI
@testable import Core

final class CursorPagingQueryTests: XCTestCase {
    func testGivenAPagingQueryWhenTheDataIsLoadedThenAPageLoadResultWithDataIsReturned() async {
        let pagingQuery = givenAPagingQuery()
        let loadResult = await whenTheDataIsLoaded(pagingQuery: pagingQuery)
        thenAPageLoadResultWithDataIsReturned(actualLoadResult: loadResult)
    }
    
    func testGivenACausingErrorPagingQueryWhenTheDataIsLoadedThenAnErrorLoadResultIsReturned() async {
        let pagingQuery = givenACausingExceptionPagingQuery()
        let loadResult = await whenTheDataIsLoaded(pagingQuery: pagingQuery)
        thenAnErrorLoadResultIsReturned(actualLoadResult: loadResult)
    }
    
    private func givenAPagingQuery() -> CursorPagingQuery<Int> {
        let pagingQuery: CursorPagingQuery<Int> = { _ in
            Result.success(
                CursorPage(
                    items: items,
                    next: next
                )
            )
        }
        return pagingQuery
    }
    
    private func whenTheDataIsLoaded(pagingQuery: @escaping CursorPagingQuery<Int>) async -> LoadResult<String, Int> {
        let cursorPagingSource = CursorPagingSource(pagingQuery: pagingQuery)
        return await cursorPagingSource.load(loadConfig: LoadConfig(key: "key"))
    }
    
    private func thenAPageLoadResultWithDataIsReturned(actualLoadResult: LoadResult<String, Int>) {
        guard case .page(let actualItems, let actualNext) = actualLoadResult else {
            XCTFail("It's not a LoadResult.page")
            return
        }
        
        XCTAssertEqual(items, actualItems)
        XCTAssertEqual(next, actualNext)
    }
    
    private func givenACausingExceptionPagingQuery() -> CursorPagingQuery<Int> {
        let pagingQuery: CursorPagingQuery<Int> = { _ in
            Result.failure(TestError())
        }
        return pagingQuery
    }
    
    private func thenAnErrorLoadResultIsReturned(actualLoadResult: LoadResult<String, Int>) {
        guard case .failure = actualLoadResult else {
            XCTFail("It's not a LoadResult.failure")
            return
        }
    }
}

private struct TestError: Error {}

private let items = [Int](0...9)
private let next: String? = nil
