public struct CombinedLoadStates: Equatable {
    public let refresh: LoadState
    public let append: LoadState

    public static func ==(lhs: CombinedLoadStates, rhs: CombinedLoadStates) -> Bool {
        return lhs.refresh == rhs.refresh && lhs.append == rhs.append
    }
}

extension CombinedLoadStates {
    mutating func setIfDifferent(to newValue: CombinedLoadStates) {
        if self != newValue {
            self = newValue
        }
    }
}
