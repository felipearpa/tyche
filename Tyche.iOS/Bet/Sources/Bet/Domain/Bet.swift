import Core

public struct Bet {
    let poolId: String
    let gamblerId: String
    let matchId: String
    let homeTeamBet: BetScore
    let awayTeamBet: BetScore
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
