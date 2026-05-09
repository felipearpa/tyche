public struct Pool {
    public let id: String
    public let name: String
    public let creatorGamblerId: String

    public init(id: String, name: String, creatorGamblerId: String) {
        self.id = id
        self.name = name
        self.creatorGamblerId = creatorGamblerId
    }
}
