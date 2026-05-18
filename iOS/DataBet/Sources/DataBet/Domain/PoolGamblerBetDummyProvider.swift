import Foundation
import Core

private let countries: [(String, String)] = [
    ("ar", "Argentina"),
    ("at", "Austria"),
    ("au", "Australia"),
    ("ba", "Bosnia and Herzegovina"),
    ("be", "Belgium"),
    ("br", "Brazil"),
    ("ca", "Canada"),
    ("cd", "Congo"),
    ("ch", "Switzerland"),
    ("ci", "Ivory Coast"),
    ("co", "Colombia"),
    ("cv", "Cape Verde"),
    ("cw", "Curaçao"),
    ("cz", "Czech Republic"),
    ("de", "Germany"),
    ("dz", "Algeria"),
    ("ec", "Ecuador"),
    ("eg", "Egypt"),
    ("es", "Spain"),
    ("fr", "France"),
    ("gb_eng", "England"),
    ("gb_sct", "Scotland"),
    ("gh", "Ghana"),
    ("hr", "Croatia"),
    ("ht", "Haiti"),
    ("iq", "Iraq"),
    ("ir", "Iran"),
    ("jo", "Jordan"),
    ("jp", "Japan"),
    ("kr", "South Korea"),
    ("ma", "Morocco"),
    ("mx", "Mexico"),
    ("nl", "Netherlands"),
    ("no", "Norway"),
    ("nz", "New Zealand"),
    ("pa", "Panama"),
    ("pt", "Portugal"),
    ("py", "Paraguay"),
    ("qa", "Qatar"),
    ("sa", "Saudi Arabia"),
    ("se", "Sweden"),
    ("sn", "Senegal"),
    ("tn", "Tunisia"),
    ("tr", "Turkey"),
    ("us", "USA"),
    ("uy", "Uruguay"),
    ("uz", "Uzbekistan"),
    ("za", "South Africa"),
]

func dummyPoolGamblerBet() -> PoolGamblerBet {
    let homeCountry = countries.randomElement()!
    var awayCountry = countries.randomElement()!
    while awayCountry == homeCountry {
        awayCountry = countries.randomElement()!
    }
    let isComputed = Bool.random()
    let isLocked = isComputed || Bool.random()

    return PoolGamblerBet(
        poolId: "pool123",
        gamblerId: "gambler456",
        gamblerUsername: "gambler456@example.com",
        matchId: "match789",
        homeTeamId: homeCountry.0,
        homeTeamName: homeCountry.1,
        awayTeamId: awayCountry.0,
        awayTeamName: awayCountry.1,
        matchScore: isComputed ? TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)) : nil,
        betScore: TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)),
        score: isComputed ? Int.random(in: 0...30) : nil,
        matchDateTime: Date(),
        isLocked: isLocked,
        isComputed: isComputed
    )
}


func poolGamblerBetDummyModels() -> [PoolGamblerBet] {
    (1...10).map { i in
        let homeCountry = countries.randomElement()!
        var awayCountry = countries.randomElement()!
        while awayCountry == homeCountry {
            awayCountry = countries.randomElement()!
        }
        let isComputed = Bool.random()
        let isLocked = isComputed || Bool.random()

        return PoolGamblerBet(
            poolId: "pool\(Int.random(in: 100...999))",
            gamblerId: "gambler\(Int.random(in: 100...999))",
            gamblerUsername: "gambler\(i)@example.com",
            matchId: "match\(Int.random(in: 100...999))",
            homeTeamId: homeCountry.0,
            homeTeamName: homeCountry.1,
            awayTeamId: awayCountry.0,
            awayTeamName: awayCountry.1,
            matchScore: isComputed ? TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)) : nil,
            betScore: TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)),
            score: Int.random(in: 0...10),
            matchDateTime: Date(),
            isLocked: isLocked,
            isComputed: isComputed
        )
    }
}

func poolGamblerBetPendingDummyModels() -> [PoolGamblerBet] {
    (1...10).map { i in
        let homeCountry = countries.randomElement()!
        var awayCountry = countries.randomElement()!
        while awayCountry == homeCountry {
            awayCountry = countries.randomElement()!
        }
        let isLocked = Bool.random()

        return PoolGamblerBet(
            poolId: "pool\(Int.random(in: 100...999))",
            gamblerId: "gambler\(Int.random(in: 100...999))",
            gamblerUsername: "gambler\(i)@example.com",
            matchId: "match\(Int.random(in: 100...999))",
            homeTeamId: homeCountry.0,
            homeTeamName: homeCountry.1,
            awayTeamId: awayCountry.0,
            awayTeamName: awayCountry.1,
            matchScore: nil,
            betScore: TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)),
            score: Int.random(in: 0...10),
            matchDateTime: Date().addingTimeInterval(-30 * 60),
            isLocked: isLocked,
            isComputed: false
        )
    }
}

func poolGamblerBetFinishedDummyModels() -> [PoolGamblerBet] {
    (1...10).map { i in
        let homeCountry = countries.randomElement()!
        var awayCountry = countries.randomElement()!
        while awayCountry == homeCountry {
            awayCountry = countries.randomElement()!
        }

        return PoolGamblerBet(
            poolId: "pool\(Int.random(in: 100...999))",
            gamblerId: "gambler\(Int.random(in: 100...999))",
            gamblerUsername: "gambler\(i)@example.com",
            matchId: "match\(Int.random(in: 100...999))",
            homeTeamId: homeCountry.0,
            homeTeamName: homeCountry.1,
            awayTeamId: awayCountry.0,
            awayTeamName: awayCountry.1,
            matchScore: TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)),
            betScore: TeamScore(homeTeamValue: Int.random(in: 0...5), awayTeamValue: Int.random(in: 0...5)),
            score: Int.random(in: 0...10),
            matchDateTime: Date(),
            isLocked: true,
            isComputed: true
        )
    }
}
