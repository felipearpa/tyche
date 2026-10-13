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

/// Finished kickoff groups, newest first: a two-match group that moved up, a one-match group
/// that moved down, and a group whose movement is unavailable.
func kickoffGroupFinishedDummyModels() -> [KickoffGroup] {
    let now = Date()
    return [
        dummyKickoffGroup(kickoff: now.addingTimeInterval(-1 * 86_400), total: 2, computed: 2, movement: (8, 5)),
        dummyKickoffGroup(kickoff: now.addingTimeInterval(-2 * 86_400), total: 1, computed: 1, movement: (4, 5)),
        dummyKickoffGroup(kickoff: now.addingTimeInterval(-3 * 86_400), total: 3, computed: 3, movement: nil),
    ]
}

/// Timeline kickoff groups: an unsettled group with one of two matches computed, then finished
/// groups.
func kickoffGroupTimelineDummyModels() -> [KickoffGroup] {
    [dummyKickoffGroup(kickoff: Date().addingTimeInterval(-2 * 3_600), total: 2, computed: 1, movement: (6, 4))]
        + kickoffGroupFinishedDummyModels()
}

private func dummyKickoffGroup(
    kickoff: Date,
    total: Int,
    computed: Int,
    movement: (Int, Int)?
) -> KickoffGroup {
    KickoffGroup(
        kickoffDateTime: kickoff,
        totalMatches: total,
        computedMatches: computed,
        movement: movement.map { GroupMovement(beforePosition: $0.0, position: $0.1) },
        matches: (0..<total).map { index in
            dummyKickoffGroupBet(index: index, kickoff: kickoff, isComputed: index < computed)
        }
    )
}

private func dummyKickoffGroupBet(index: Int, kickoff: Date, isComputed: Bool) -> PoolGamblerBet {
    let homeCountry = countries[(index * 2) % countries.count]
    let awayCountry = countries[(index * 2 + 1) % countries.count]
    return PoolGamblerBet(
        poolId: "pool123",
        gamblerId: "gambler456",
        gamblerUsername: "gambler456@example.com",
        matchId: "match\(Int(kickoff.timeIntervalSince1970))-\(index)",
        homeTeamId: homeCountry.0,
        homeTeamName: homeCountry.1,
        awayTeamId: awayCountry.0,
        awayTeamName: awayCountry.1,
        matchScore: isComputed ? TeamScore(homeTeamValue: index, awayTeamValue: 1) : nil,
        betScore: TeamScore(homeTeamValue: 1, awayTeamValue: 1),
        score: isComputed ? index * 2 : nil,
        matchDateTime: kickoff,
        isLocked: true,
        isComputed: isComputed
    )
}
