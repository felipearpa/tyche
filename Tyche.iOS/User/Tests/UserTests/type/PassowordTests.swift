import XCTest
@testable import User

final class PasswordTests: XCTestCase {
    
    func testGivenAValidPasswordStringWhenAPasswordIsCreatedThenAPasswordWithTheStringContainedIsReturned() throws {
        try [ "Felipearpa1#",
          "#felipeArpa1",
          "1Felipearpa#" ].forEach { value in
            try givenAValidPasswordStringWhenAPasswordIsCreatedThenAPasswordWithTheStringContainedIsReturned(value: value)
        }
    }
    
    private func givenAValidPasswordStringWhenAPasswordIsCreatedThenAPasswordWithTheStringContainedIsReturned(value: String) throws {
        let password = Password(value: value)!
        XCTAssertEqual(value, password.value)
    }
    
    func testGivenAnInvalidPasswordStringWhenAPasswordIsCreatedThenANilValueIsGotten() throws {
        try [ "",
          "felipe",
          "Felipe",
          "Felipe#",
          "Felipe1" ].forEach { value in
            try givenAnInvalidPasswordStringWhenAPasswordIsCreatedThenANilValueIsGotten(value: value)
        }
    }
    
    private func givenAnInvalidPasswordStringWhenAPasswordIsCreatedThenANilValueIsGotten(value: String) throws {
        let maybePassoword = Password(value: value)
        XCTAssertNil(maybePassoword)
    }
}
