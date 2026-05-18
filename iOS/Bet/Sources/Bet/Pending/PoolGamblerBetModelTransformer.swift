extension PoolGamblerBetModel {
    func toPartialPoolGamblerBet() -> PartialPoolGamblerBetModel {
        PartialPoolGamblerBetModel(
            homeTeamBet: self.homeTeamBetRawValue(),
            awayTeamBet: self.awayTeamBetRawValue()
        )
    }
}
