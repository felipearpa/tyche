import Foundation

public struct MatchBetListViewRoute: Hashable {
    public let poolId: String
    public let matchId: String
    public let homeTeamName: String
    public let awayTeamName: String
    public let matchDateTime: Date
    public let homeTeamScore: Int?
    public let awayTeamScore: Int?
    public let isLive: Bool

    public init(
        poolId: String,
        matchId: String,
        homeTeamName: String,
        awayTeamName: String,
        matchDateTime: Date,
        homeTeamScore: Int?,
        awayTeamScore: Int?,
        isLive: Bool
    ) {
        self.poolId = poolId
        self.matchId = matchId
        self.homeTeamName = homeTeamName
        self.awayTeamName = awayTeamName
        self.matchDateTime = matchDateTime
        self.homeTeamScore = homeTeamScore
        self.awayTeamScore = awayTeamScore
        self.isLive = isLive
    }
}
