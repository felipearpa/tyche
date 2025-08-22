public struct CombinedLoadStates: Equatable {
    public let refresh: LoadState
    public let append: LoadState

    public static func ==(lhs: CombinedLoadStates, rhs: CombinedLoadStates) -> Bool {
        return lhs.refresh == rhs.refresh && lhs.append == rhs.append
    }
}
