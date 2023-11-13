import Foundation
import Core

public struct PoolGamblerBet: Codable {
    let poolId: String
    let gamblerId: String
    let matchId: String
    let homeTeamId: String
    let homeTeamName: String
    let awayTeamId: String
    let awayTeamName: String
    let matchScore: TeamScore<Int>?
    let betScore: TeamScore<Int>?
    let score: Int?
    let matchDateTime: Date
    let isLocked: Bool
}

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
