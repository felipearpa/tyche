public struct PoolFromLayoutCreatorRoute : Hashable {
    public let preselectedPoolLayoutId: String?
    public let preselectedPoolName: String?

    public init(
        preselectedPoolLayoutId: String? = nil,
        preselectedPoolName: String? = nil,
    ) {
        self.preselectedPoolLayoutId = preselectedPoolLayoutId
        self.preselectedPoolName = preselectedPoolName
    }
}
