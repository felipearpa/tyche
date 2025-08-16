import XCTest
@testable import Core

final class EmailTests: XCTestCase {
    func testGivenAValidStringWhenAnEmailIsCreatedThenAnEmailContainingTheStringIsReturned() {
        ["mail@felipearpa.com", "mail@felipearpa.com.co"].forEach { rawEmail in
            let email = Email(rawEmail)
            XCTAssertEqual(rawEmail, email?.value)
        }
    }
    
    func testGivenAnInvalidStringWhenAnEmailIsCreatedThenNilIsReturned() {
        ["", "invalid"].forEach { rawEmail in
            let email = Email(rawEmail)
            XCTAssertNil(email)
        }
    }
}
