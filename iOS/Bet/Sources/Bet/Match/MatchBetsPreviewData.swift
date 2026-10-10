import Core
import Foundation

/// Preview and test fixtures for the match screen. Sample data, never shown as real content.

let matchPreviewViewerId = "preview-viewer"

/// The heading in each state: open betting, closed without and with a score, computed, and a
/// computed match whose result is unavailable.
func matchHeaderPreviewCases() -> [(String, PoolGamblerBetModel, MatchBetsPresentation)] {
    let base = matchPreviewBaseModel()
    let open = base.copy {
        $0.matchScore = nil
        $0.isComputed = false
    }
    let closed = base.copy {
        $0.matchScore = nil
        $0.isComputed = false
        $0.lock()
    }
    let closedWithScore = closed.copy { $0.matchScore = TeamScore(homeTeamValue: 1, awayTeamValue: 0) }
    let computed = base.copy {
        $0.isComputed = true
        $0.lock()
    }
    let computedUnavailable = computed.copy { $0.matchScore = nil }
    return [
        ("open", open, .hidden),
        ("closed", closed, .betsClosed),
        ("closed-score", closedWithScore, .betsClosed),
        ("computed", computed, .computed),
        ("computed-unavailable", computedUnavailable, .computed),
    ]
}

func matchPreviewBaseModel() -> PoolGamblerBetModel {
    poolGamblerBetDummyModel().copy {
        $0.homeTeamId = "es"
        $0.homeTeamName = "España"
        $0.awayTeamId = "ar"
        $0.awayTeamName = "Argentina"
        $0.matchScore = TeamScore(homeTeamValue: 0, awayTeamValue: 0)
        $0.betScore = TeamScore(homeTeamValue: 3, awayTeamValue: 2)
    }
}

/// Participants in pool-total order: a leader without a bet, a zero award above a positive one,
/// the viewer in their ranked place, an unavailable award, and a long name.
func matchParticipantPreviewModels(isComputed: Bool) -> [PoolGamblerBetModel] {
    let base = matchPreviewBaseModel().copy {
        $0.isComputed = isComputed
        $0.lock()
    }
    let rows: [(String, String, TeamScore<Int>?, Int?)] = [
        ("p1", "El mono", nil, 0),
        ("p2", "Val Cardo", TeamScore(homeTeamValue: 3, awayTeamValue: 1), 0),
        ("p3", "Juano", TeamScore(homeTeamValue: 1, awayTeamValue: 0), 4),
        ("p4", "Santiago Arcila", TeamScore(homeTeamValue: 5, awayTeamValue: 0), 4),
        (matchPreviewViewerId, "felipearpa", TeamScore(homeTeamValue: 3, awayTeamValue: 2), 0),
        ("p6", "giraldocano517@gmail.com", nil, nil),
        ("p7", "Juan Pablo Rojas de la Santísima Trinidad", TeamScore(homeTeamValue: 2, awayTeamValue: 3), 1),
    ]
    return rows.map { gamblerId, name, bet, award in
        base.copy {
            $0.gamblerId = gamblerId
            $0.gamblerUsername = name
            $0.betScore = bet
            $0.score = isComputed ? award : nil
        }
    }
}
