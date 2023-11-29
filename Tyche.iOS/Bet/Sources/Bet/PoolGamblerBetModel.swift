import Foundation
import Core

struct PoolGamblerBetModelId: Hashable, Codable {
    let poolId: String
    let gamblerId: String
    let matchId: String
}

struct PoolGamblerBetModel: Codable, Identifiable, Hashable {
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
    fileprivate(set) var isLocked: Bool
    
    var id: PoolGamblerBetModelId {
        return PoolGamblerBetModelId(poolId: self.poolId, gamblerId: self.gamblerId, matchId: self.matchId)
    }
}

extension PoolGamblerBetModel {
    func copy(_ build: (inout Builder) -> Void) -> PoolGamblerBetModel {
        var builder = Builder(original: self)
        build(&builder)
        return builder.build()
    }
    
    struct Builder {
        var poolId: String
        var gamblerId: String
        var matchId: String
        var homeTeamId: String
        var homeTeamName: String
        var awayTeamId: String
        var awayTeamName: String
        var matchScore: TeamScore<Int>?
        var betScore: TeamScore<Int>?
        var score: Int?
        var matchDateTime: Date
        fileprivate var isLocked: Bool
        
        fileprivate init(original: PoolGamblerBetModel) {
            self.poolId = original.poolId
            self.gamblerId = original.gamblerId
            self.matchId = original.matchId
            self.homeTeamId = original.homeTeamId
            self.homeTeamName = original.homeTeamName
            self.awayTeamId = original.awayTeamId
            self.awayTeamName = original.awayTeamName
            self.matchScore = original.matchScore
            self.betScore = original.betScore
            self.score = original.score
            self.matchDateTime = original.matchDateTime
            self.isLocked = original.isLocked
        }
        
        fileprivate func build() -> PoolGamblerBetModel {
            PoolGamblerBetModel(
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
    
    func homeTeamBetRawValue() -> String {
        if let homeTeamBet = self.betScore?.homeTeamValue { String(homeTeamBet) } else { "" }
    }
    
    func awayTeamBetRawValue() -> String {
        if let awayTeamBet = self.betScore?.awayTeamValue { String(awayTeamBet) } else { "" }
    }
    
    mutating func lock() {
        self.isLocked = true
    }
}
