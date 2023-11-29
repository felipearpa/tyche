import Foundation
import Core

func dummyPoolGamblerBet() -> PoolGamblerBet {
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


func dummyPoolGamblerBets() -> [PoolGamblerBet] {
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
        )
    ]
}
