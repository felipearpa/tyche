import DataBet

extension PoolGamblerBet {
    func toPoolGamblerBetModel() -> PoolGamblerBetModel {
        return PoolGamblerBetModel(
            poolId: self.poolId,
            gamblerId: self.gamblerId,
            matchId: self.matchId,
            homeTeamId: self.homeTeamId,
            homeTeamName: self.homeTeamName,
            awayTeamId: self.awayTeamId,
            awayTeamName: self.awayTeamName,
            matchScore: self.matchScore,
            betScore: self.betScore,
            score: self.score,
            matchDateTime: self.matchDateTime,
            isLocked: self.isLocked
        )
    }
}
