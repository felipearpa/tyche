import XCTest
import ViewInspector
@testable import Pool

final class PoolTests: XCTestCase {
    func testRankMovementClassificationInputs() {
        XCTAssertEqual(score(position: 2, beforePosition: 5).rank(), 3)
        XCTAssertEqual(score(position: 5, beforePosition: 2).rank(), -3)
        XCTAssertEqual(score(position: 2, beforePosition: 2).rank(), 0)
    }

    func testRankMovementIsUnknownWhenEitherPositionIsMissing() {
        XCTAssertNil(score(position: nil, beforePosition: 2).rank())
        XCTAssertNil(score(position: 2, beforePosition: nil).rank())
    }

    func testPositionIndicatorSupportsDefaultLeaderboardAndMissingStates() throws {
        XCTAssertEqual(
            try PostionIndicator(
                position: 1,
                shouldUsePrimeryColor: false
            )
            .inspect()
            .find(ViewType.Text.self)
            .string(),
            "1"
        )

        XCTAssertEqual(
            try PostionIndicator(
                position: nil,
                shouldUsePrimeryColor: false,
                size: 44,
                cornerRadius: 10
            )
            .inspect()
            .find(ViewType.Text.self)
            .string(),
            "—"
        )
    }

    func testCurrentRowExposesYouScoreAndMovement() throws {
        let strings = try GamblerScoreItem(
            poolGamblerScore: score(
                username: "ElGoleador",
                position: 3,
                beforePosition: 4,
                score: 150
            ),
            isCurrentUser: true
        )
        .inspect()
        .findAll(ViewType.Text.self)
        .map { try $0.string() }

        XCTAssertTrue(strings.contains("3"))
        XCTAssertTrue(strings.contains("ElGoleador"))
        XCTAssertTrue(strings.contains("You"))
        XCTAssertTrue(strings.contains("150"))
        XCTAssertTrue(strings.contains("1"))
    }

    func testMissingFieldsKeepDashesAndLongIdentityText() throws {
        let username = "A very long username that must remain available to VoiceOver"
        let strings = try GamblerScoreItem(
            poolGamblerScore: score(
                username: username,
                position: nil,
                beforePosition: nil,
                score: nil
            ),
            isCurrentUser: false
        )
        .inspect()
        .findAll(ViewType.Text.self)
        .map { try $0.string() }

        XCTAssertTrue(strings.contains(username))
        XCTAssertEqual(strings.filter { $0 == "—" }.count, 2)
    }

    private func score(
        username: String = "Gambler",
        position: Int?,
        beforePosition: Int?,
        score: Int? = nil
    ) -> PoolGamblerScoreModel {
        PoolGamblerScoreModel(
            poolId: "pool",
            poolName: "Pool",
            gamblerId: "gambler",
            gamblerUsername: username,
            position: position,
            beforePosition: beforePosition,
            score: score,
            gamblerCount: 10
        )
    }
}
