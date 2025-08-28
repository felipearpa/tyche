// ^ and $ are the start and end anchors, respectively, to match the entire string from start to end.
// [A-Za-z0-9._%+-]+ matches one or more of the following characters: uppercase letters, lowercase letters, digits, period (.), underscore (_), percent sign (%), plus sign (+), or hyphen/minus sign (-). This corresponds to the local part of the email address before the @ symbol.
// @ matches the literal @ symbol.
// [A-Za-z0-9.-]+ matches one or more of the following characters: uppercase letters, lowercase letters, digits, period (.), or hyphen/minus sign (-). This corresponds to the domain part of the email address after the @ symbol.
// \. matches the literal . character. It needs to be escaped with a backslash (\) because . has a special meaning in regex.
// [A-Za-z]{2,} matches two or more uppercase or lowercase letters. This corresponds to the top-level domain (TLD) part of the email address (e.g., com, org, net).
private let pattern = #/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/#

private enum EmailError: Error {
    case invalidPattern
}

public struct Email {
    public let value: String
    
    public init?(_ value: String) {
        if !Email.isValid(value) {
            return nil
        }
        self.value = value
    }
    
    public static func isValid(_ value: String) -> Bool {
        do {
            try checkPattern(value)
            return true
        } catch {
            return false
        }
    }
    
    private static func checkPattern(_ value: String) throws {
        if !value.contains(pattern) {
            throw EmailError.invalidPattern
        }
    }
}
