import XCTest
@testable import Core

final class CursorPagingTests: XCTestCase {
    
    func testGivenACursorPageAndATransformWhenIsMappedThenANewCursorPageIsReturned() throws {
        let sourcePage = CursorPage(items: Array(0...10), next: nil)
        let transform: (Int) -> Int = { number in number * 2 }
        
        let newPage = sourcePage.map(transform)
        
        for i in 0...10 {
            XCTAssertEqual(transform(sourcePage.items[i]), newPage.items[i])
        }
    }
}
