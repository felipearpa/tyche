import Core

public struct Bet {
    public let poolId: String
    public let gamblerId: String
    public let matchId: String
    public let homeTeamBet: BetScore
    public let awayTeamBet: BetScore
    
    public init(
        poolId: String,
        gamblerId: String,
        matchId: String,
        homeTeamBet: BetScore,
        awayTeamBet: BetScore
    ) {
        self.poolId = poolId
        self.gamblerId = gamblerId
        self.matchId = matchId
        self.homeTeamBet = homeTeamBet
        self.awayTeamBet = awayTeamBet
    }
}

extension Bet {
    func toBetRequest() -> BetRequest {
        BetRequest(
            poolId: self.poolId,
            gamblerId: self.gamblerId,
            matchId: self.matchId,
            homeTeamBet: self.homeTeamBet.value,
            awayTeamBet: self.awayTeamBet.value
        )
    }
}
