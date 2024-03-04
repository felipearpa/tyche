import XCTest
@testable import Core

private let next = "next"
private let cursorPage = CursorPage(items: [1, 2, 3], next: "next")
private let transform: (Int) -> Int = { number in number * 2 }

final class CursorPagingTests: XCTestCase {
    func testGivenACursorPageAndATransformFunctionWhenIsMappedThenAMappedCursorPageIsReturned() throws {
        let newCursorPage = cursorPage.map(transform)
        verifyTheMappedCursorPage(actualCursorPage: newCursorPage)
    }
    
    private func verifyTheMappedCursorPage(actualCursorPage: CursorPage<Int>) {
        for i in 0..<cursorPage.items.count {
            XCTAssertEqual(transform(cursorPage.items[i]), actualCursorPage.items[i])
        }
        
        XCTAssertEqual(next, actualCursorPage.next)
    }
}
