import Foundation
import Core

func poolGamblerBetPlaceholderModel(
    isLocked: Bool = true,
    isComputed: Bool = false
) -> PoolGamblerBetModel {
    PoolGamblerBetModel(
        poolId: String(repeating: "X", count: 15),
        gamblerId: String(repeating: "X", count: 15),
        gamblerUsername: String(repeating: "X", count: 25),
        matchId: String(repeating: "X", count: 15),
        homeTeamId: "co",
        homeTeamName: String(repeating: "X", count: 25),
        awayTeamId: "br",
        awayTeamName: String(repeating: "X", count: 25),
        matchScore: TeamScore(homeTeamValue: 100, awayTeamValue: 100),
        betScore: TeamScore(homeTeamValue: 100, awayTeamValue: 100),
        score: 10,
        matchDateTime: Date(),
        isLocked: isLocked,
        isComputed: isComputed
    )
}

func poolGamblerBetDummyModel() -> PoolGamblerBetModel {
    return PoolGamblerBetModel(
        poolId: "pool123",
        gamblerId: "gambler456",
        gamblerUsername: "gambler456@example.com",
        matchId: "match789",
        homeTeamId: "co",
        homeTeamName: "Red Devils",
        awayTeamId: "br",
        awayTeamName: "Blue Angels",
        matchScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2),
        betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
        score: 5,
        matchDateTime: Date(),
        isLocked: false,
        isComputed: true
    )
}

func poolGamblerBetDummyModels() -> [PoolGamblerBetModel] {
    return [
        PoolGamblerBetModel(
            poolId: "pool123",
            gamblerId: "gambler456",
            gamblerUsername: "gambler456@example.com",
            matchId: "match789",
            homeTeamId: "homeTeam1011",
            homeTeamName: "Red Devils",
            awayTeamId: "awayTeam1213",
            awayTeamName: "Blue Angels",
            matchScore: TeamScore(homeTeamValue: 3, awayTeamValue: 2),
            betScore: TeamScore(homeTeamValue: 2, awayTeamValue: 2),
            score: 5,
            matchDateTime: Date(),
            isLocked: false,
            isComputed: true
        ),
        PoolGamblerBetModel(
            poolId: "pool124",
            gamblerId: "gambler457",
            gamblerUsername: "gambler457@example.com",
            matchId: "match790",
            homeTeamId: "homeTeam1012",
            homeTeamName: "Silver Surfers",
            awayTeamId: "awayTeam1214",
            awayTeamName: "Golden Gladiators",
            matchScore: TeamScore(homeTeamValue: 1, awayTeamValue: 1),
            betScore: TeamScore(homeTeamValue: 1, awayTeamValue: 1),
            score: 2,
            matchDateTime: Date(),
            isLocked: false,
            isComputed: true
        ),
        PoolGamblerBetModel(
            poolId: "pool125",
            gamblerId: "gambler458",
            gamblerUsername: "gambler458@example.com",
            matchId: "match791",
            homeTeamId: "homeTeam1013",
            homeTeamName: "Bronze Beasts",
            awayTeamId: "awayTeam1215",
            awayTeamName: "Platinum Pumas",
            matchScore: TeamScore(homeTeamValue: 0, awayTeamValue: 2),
            betScore: TeamScore(homeTeamValue: 1, awayTeamValue: 2),
            score: 3,
            matchDateTime: Date(),
            isLocked: false,
            isComputed: true
        )
    ]
}

/// History preview rows covering a positive award, a zero award, a missing bet, unavailable
/// result and points, a long team name, and a match from an earlier year.
func historyBetPreviewModels() -> [PoolGamblerBetModel] {
    let base = poolGamblerBetDummyModel()
    let lastYear = Calendar.current.date(byAdding: .year, value: -1, to: Date()) ?? Date()
    return [
        base.copy {
            $0.matchId = "history-positive"
            $0.homeTeamId = "gb_eng"
            $0.homeTeamName = "Inglaterra"
            $0.awayTeamId = "ar"
            $0.awayTeamName = "Argentina"
            $0.matchScore = TeamScore(homeTeamValue: 1, awayTeamValue: 2)
            $0.betScore = TeamScore(homeTeamValue: 2, awayTeamValue: 1)
            $0.score = 2
        },
        base.copy {
            $0.matchId = "history-zero"
            $0.homeTeamId = "es"
            $0.homeTeamName = "España"
            $0.awayTeamId = "ar"
            $0.awayTeamName = "Argentina"
            $0.matchScore = TeamScore(homeTeamValue: 0, awayTeamValue: 0)
            $0.betScore = TeamScore(homeTeamValue: 3, awayTeamValue: 2)
            $0.score = 0
        },
        base.copy {
            $0.matchId = "history-long-name"
            $0.homeTeamId = "pt"
            $0.homeTeamName = "Portugal"
            $0.awayTeamId = "cd"
            $0.awayTeamName = "República Democrática del Congo"
            $0.matchScore = TeamScore(homeTeamValue: 1, awayTeamValue: 1)
            $0.betScore = nil
            $0.score = 1
        },
        base.copy {
            $0.matchId = "history-unavailable"
            $0.homeTeamId = "us"
            $0.homeTeamName = "Estados Unidos"
            $0.awayTeamId = "py"
            $0.awayTeamName = "Paraguay"
            $0.matchScore = nil
            $0.betScore = nil
            $0.score = nil
            $0.matchDateTime = lastYear
        },
    ]
}

/// Stable filler for History's row placeholders, sized like a typical finished match so loading
/// rows keep the loaded rows' height. Never shown as real content.
func historyBetPlaceholderModel() -> PoolGamblerBetModel {
    poolGamblerBetPlaceholderModel(isLocked: true, isComputed: true).copy {
        $0.homeTeamName = String(repeating: "X", count: 8)
        $0.awayTeamName = String(repeating: "X", count: 8)
        $0.matchScore = TeamScore(homeTeamValue: 0, awayTeamValue: 0)
        $0.betScore = TeamScore(homeTeamValue: 0, awayTeamValue: 0)
        $0.score = 0
    }
}
