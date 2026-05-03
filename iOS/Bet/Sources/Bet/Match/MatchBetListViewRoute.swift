import Foundation

public struct MatchBetListViewRoute: Hashable {
    public let poolId: String
    public let gamblerId: String
    public let matchId: String

    public init(
        poolId: String,
        gamblerId: String,
        matchId: String
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.matchId = matchId
    }
}
