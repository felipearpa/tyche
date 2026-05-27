public struct Pool {
    public let id: String
    public let name: String
    public let creatorGamblerId: String
    public let gamblerCount: Int?

    public init(id: String, name: String, creatorGamblerId: String, gamblerCount: Int? = nil) {
        self.id = id
        self.name = name
        self.creatorGamblerId = creatorGamblerId
        self.gamblerCount = gamblerCount
    }
}
