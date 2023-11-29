import Foundation
import Core

struct PoolGamblerBetResponse: Codable {
    let poolId: String
    let gamblerId: String
    let matchId: String
    let homeTeamId: String
    let homeTeamName: String
    let homeTeamScore: Int?
    let homeTeamBet: Int?
    let awayTeamId: String
    let awayTeamName: String
    let awayTeamScore: Int?
    let awayTeamBet: Int?
    let score: Int?
    let matchDateTime: Date
    let isLocked: Bool
}

extension PoolGamblerBetResponse {
    func toPoolGamblerBet() -> PoolGamblerBet {
        return PoolGamblerBet(
            poolId: self.poolId,
            gamblerId: self.gamblerId,
            matchId: self.matchId,
            homeTeamId: self.homeTeamId,
            homeTeamName: self.homeTeamName,
            awayTeamId: self.awayTeamId,
            awayTeamName: self.awayTeamName,
            matchScore: self.homeTeamScore == nil || self.awayTeamScore == nil ?
            nil :
                TeamScore(homeTeamValue: self.homeTeamScore!, awayTeamValue: self.awayTeamScore!),
            betScore: self.homeTeamBet == nil || self.awayTeamBet == nil ?
            nil :
                TeamScore(homeTeamValue: self.homeTeamBet!, awayTeamValue: self.awayTeamBet!),
            score: self.score,
            matchDateTime: self.matchDateTime,
            isLocked: self.isLocked
        )
    }
}
