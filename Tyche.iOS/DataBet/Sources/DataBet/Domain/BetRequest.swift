struct BetRequest: Codable {
    let poolId: String
    let gamblerId: String
    let matchId: String
    let homeTeamBet: Int
    let awayTeamBet: Int
}
