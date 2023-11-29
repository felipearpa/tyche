import Foundation

// * The password must contain at least one digit ((?=.*\d)).
// * The password must contain at least one lowercase letter ((?=.*[a-z])).
// * The password must contain at least one uppercase letter ((?=.*[A-Z])).
// * The password must contain at least one non-word character (i.e. a special character) ((?=.*[^\w\d\s:])).
// * The password must be between 8 and 16 characters in length (([^\s]){8,16}).
private let passwordRegularExpression = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^\\w\\d\\s:])([^\\s]){8,16}$"

enum PasswordError: Error {
    case empty
    case notPattern
}

public struct Password {
    let value: String
    
    public init?(_ value: String) {
        if !Password.isValid(value: value) {
            return nil
        }
        self.value = value
    }
    
    public static func isValid(value: String) -> Bool {
        do {
            try checkEmpty(value: value)
            try checkPasswordPattern(value: value)
            return true
        } catch {
            return false
        }
    }
    
    private static func checkEmpty(value: String) throws {
        if value.isEmpty {
            throw PasswordError.empty
        }
    }
    
    private static func checkPasswordPattern(value: String) throws {
        let pattern = try! NSRegularExpression(pattern: passwordRegularExpression)
        let range = NSRange(location: 0, length: value.utf16.count)
        if pattern.firstMatch(in: value, options: [], range: range) == nil {
            throw PasswordError.notPattern
        }
    }
}
