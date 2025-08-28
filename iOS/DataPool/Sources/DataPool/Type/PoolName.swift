private enum PoolNameError: Error {
    case invalidLength
}

public struct PoolName {
    public let value: String
    
    public init?(_ value: String) {
        if !PoolName.isValid(value) {
            return nil
        }
        self.value = value
    }
    
    public static func isValid(_ value: String) -> Bool {
        do {
            try checkLength(value)
            return true
        } catch {
            return false
        }
    }
    
    private static func checkLength(_ value: String) throws {
        if !(!value.isEmpty && value.count <= 100) {
            throw PoolNameError.invalidLength
        }
    }
}
