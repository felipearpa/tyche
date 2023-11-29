import Foundation

// * The username may only contain letters, numbers, underscores, and hyphens ([a-zA-Z0-9_-]).
// * The username must be between 3 and 16 characters in length ({3,16}).
private let usernameRegularExpression = "^[a-zA-Z0-9_-]{3,16}$"

enum UsernameError: Error {
    case empty
    case notPattern
}

public struct Username {
    let value: String
    
    public init?(_ value: String) {
        if !Username.isValid(value: value) {
            return nil
        }
        self.value = value
    }
    
    public static func isValid(value: String) -> Bool {
        do {
            try checkEmpty(value: value)
            try checkUsernamePattern(value: value)
            return true
        } catch {
            return false
        }
    }
    
    private static func checkEmpty(value: String) throws {
        if value.isEmpty {
            throw UsernameError.empty
        }
    }
    
    private static func checkUsernamePattern(value: String) throws {
        let pattern = try! NSRegularExpression(pattern: usernameRegularExpression)
        let range = NSRange(location: 0, length: value.utf16.count)
        if pattern.firstMatch(in: value, options: [], range: range) == nil {
            throw UsernameError.notPattern
        }
    }
}
