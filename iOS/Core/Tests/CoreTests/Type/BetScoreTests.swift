import XCTest
@testable import Core

final class BetScoreTests: XCTestCase {
    func testGivenAValidValueWhenABetScoreIsCreatedThenTheBetScoreIsReturned() {
        [0, 500, 999].forEach { rawBetScore in
            let betScore = BetScore(rawBetScore)
            XCTAssertEqual(rawBetScore, betScore?.value)
        }
    }
    
    func testGivenAnInvalidValueWhenABetScoreIsCreatedThenNilIsReturned() {
        [-1, 1000].forEach { rawBetScore in
            let betScore = BetScore(rawBetScore)
            XCTAssertNil(betScore)
        }
    }
}
