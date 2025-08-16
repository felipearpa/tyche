private enum BetScoreError: Error {
    case notInRange
}

public struct BetScore {
    public let value: Int
    
    public init?(_ value: Int) {
        if !BetScore.isValid(value: value) {
            return nil
        }
        self.value = value
    }
    
    public static func isValid(value: Int) -> Bool {
        do {
            try checkRange(value: value)
            return true
        } catch {
            return false
        }
    }
    
    private static func checkRange(value: Int) throws {
        if !(0...999 ~= value) {
            throw BetScoreError.notInRange
        }
    }
}
