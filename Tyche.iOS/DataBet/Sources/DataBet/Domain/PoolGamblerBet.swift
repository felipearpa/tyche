import Foundation
import Core

public struct PoolGamblerBet: Codable {
    public let poolId: String
    public let gamblerId: String
    public let matchId: String
    public let homeTeamId: String
    public let homeTeamName: String
    public let awayTeamId: String
    public let awayTeamName: String
    public let matchScore: TeamScore<Int>?
    public let betScore: TeamScore<Int>?
    public let score: Int?
    public let matchDateTime: Date
    public let isLocked: Bool
}
