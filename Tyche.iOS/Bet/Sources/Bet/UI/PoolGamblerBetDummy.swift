import Foundation
import Core

func poolGamblerBet() -> PoolGamblerBet {
    return PoolGamblerBet(
        poolId: "pool123",
        gamblerId: "gambler456",
        matchId: "match789",
        homeTeamId: "homeTeam1011",
        homeTeamName: "Red Devils",
        awayTeamId: "awayTeam1213",
        awayTeamName: "Blue Angels",
        matchScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2),
        betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
        score: 5,
        matchDateTime: Date(),
        isLocked: false
    )
}

func poolGamblerBets() -> [PoolGamblerBet] {
    return [
        PoolGamblerBet(
            poolId: "pool123",
            gamblerId: "gambler456",
            matchId: "match789",
            homeTeamId: "homeTeam1011",
            homeTeamName: "Red Devils",
            awayTeamId: "awayTeam1213",
            awayTeamName: "Blue Angels",
            matchScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2),
            betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
            score: 5,
            matchDateTime: Date(),
            isLocked: false
        ),
        PoolGamblerBet(
            poolId: "pool124",
            gamblerId: "gambler457",
            matchId: "match790",
            homeTeamId: "homeTeam1012",
            homeTeamName: "Silver Surfers",
            awayTeamId: "awayTeam1214",
            awayTeamName: "Golden Gladiators",
            matchScore: TeamScore(homeTeamValue: 1, awayTeamValue: 1),
            betScore: TeamScore(homeTeamValue: 1, awayTeamValue: 1),
            score: 2,
            matchDateTime: Date(),
            isLocked: false
        ),
        PoolGamblerBet(
            poolId: "pool125",
            gamblerId: "gambler458",
            matchId: "match791",
            homeTeamId: "homeTeam1013",
            homeTeamName: "Bronze Beasts",
            awayTeamId: "awayTeam1215",
            awayTeamName: "Platinum Pumas",
            matchScore: TeamScore(homeTeamValue: 0, awayTeamValue: 2),
            betScore: TeamScore(homeTeamValue: 1, awayTeamValue: 2),
            score: 3,
            matchDateTime: Date(),
            isLocked: false
        ),
        PoolGamblerBet(
            poolId: "pool126",
            gamblerId: "gambler459",
            matchId: "match792",
            homeTeamId: "homeTeam1014",
            homeTeamName: "Crystal Chimeras",
            awayTeamId: "awayTeam1216",
            awayTeamName: "Diamond Dragons",
            matchScore: TeamScore(homeTeamValue: 2, awayTeamValue: 3),
            betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
            score: 4,
            matchDateTime: Date(),
            isLocked: false
        )
    ]
}

func poolGamblerBetModel() -> PoolGamblerBetModel {
    return PoolGamblerBetModel(
        poolId: "pool123",
        gamblerId: "gambler456",
        matchId: "match789",
        homeTeamId: "homeTeam1011",
        homeTeamName: "Red Devils",
        awayTeamId: "awayTeam1213",
        awayTeamName: "Blue Angels",
        matchScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2),
        betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
        score: 5,
        matchDateTime: Date(),
        isLocked: false
    )
}

func poolGamblerBetModels() -> [PoolGamblerBetModel] {
    return poolGamblerBets().map { poolGamblerBet in
        poolGamblerBet.toPoolGamblerBetModel()
    }
}

func fakePoolGamblerBetModel() -> PoolGamblerBetModel {
    return PoolGamblerBetModel(
        poolId: UUID().uuidString,
        gamblerId: UUID().uuidString,
        matchId: UUID().uuidString,
        homeTeamId: UUID().uuidString,
        homeTeamName: String(repeating: "X", count: 25),
        awayTeamId: UUID().uuidString,
        awayTeamName: String(repeating: "X", count: 25),
        matchScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2),
        betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
        score: 10,
        matchDateTime: Date(),
        isLocked: false
    )
}
