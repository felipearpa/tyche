import XCTest
@testable import User

final class UsernameTests: XCTestCase {
    
    func testGivenAValidUsernameStringWhenAnUsernameIsCreatedThenAnUsernameWithTheStringContainedIsReturned() throws {
        try [ "felipearpa",
          "felipearpa-1",
          "felipearpa_1" ].forEach { value in
            try givenAValidUsernameStringWhenAnUsernameIsCreatedThenAnUsernameWithTheStringContainedIsReturned(value: value)
        }
    }
    
    private func givenAValidUsernameStringWhenAnUsernameIsCreatedThenAnUsernameWithTheStringContainedIsReturned(value: String) throws {
        let username = Username(value: value)!
        XCTAssertEqual(value, username.value)
    }
    
    func testGivenAnInvalidUsernameStringWhenAnUsernameIsCreatedThenANilValueIsGotten() throws {
        try [ "",
          "mi",
          "felipearpa.",
          "felipearpa#",
          "usernameverylong1" ].forEach { value in
            try givenAnInvalidUsernameStringWhenAnUsernameIsCreatedThenANilValueIsGotten(value: value)
        }
    }
    
    private func givenAnInvalidUsernameStringWhenAnUsernameIsCreatedThenANilValueIsGotten(value: String) throws {
        let maybeUsername = Username(value: value)
        XCTAssertNil(maybeUsername)
    }
}
