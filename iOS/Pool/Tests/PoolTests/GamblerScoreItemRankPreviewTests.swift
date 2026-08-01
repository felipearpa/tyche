import XCTest
import ViewInspector
@testable import Pool

/// Rendered regression guard for the username editor's pool preview.
///
/// The preview embeds the production `GamblerScoreItem` with `position = 1` and
/// `beforePosition = 2`, so the production rank rail renders the rank tile `1` and an upward
/// movement of one place. These tests render that row the way the editor does and prove the
/// complete rank rail stays visible for every draft — including an empty-draft placeholder and
/// a very long username — and that the row's accessibility description exposes the localized
/// rank and movement. The editor's save-lifecycle states (saving, failure) do not change the
/// preview model, so the rail is invariant across them; only the draft username varies, which
/// is what these cases exercise.
final class GamblerScoreItemRankPreviewTests: XCTestCase {
    func testRankTileAndUpwardMovementRenderForANormalDraft() throws {
        let texts = try renderedTexts(username: "neptune-player")
        // The rank tile and the upward-movement indicator each render "1".
        XCTAssertEqual(texts.filter { $0 == "1" }.count, 2)
    }

    func testRankTileAndUpwardMovementRenderForAnEmptyDraftPlaceholder() throws {
        let texts = try renderedTexts(username: "Your username")
        XCTAssertEqual(texts.filter { $0 == "1" }.count, 2)
    }

    func testRankRailSurvivesALongUsername() throws {
        // A long username must yield/truncate rather than push the rank rail out of the row.
        let longName = String(repeating: "very-long-name-", count: 10)
        let texts = try renderedTexts(username: longName)
        XCTAssertEqual(
            texts.filter { $0 == "1" }.count,
            2,
            "rank tile and movement must remain rendered alongside a long username"
        )
        XCTAssertTrue(texts.contains(longName))
    }

    func testUsernameTileTruncatesToASingleLine() throws {
        // The rank rail has a fixed width; the username is the element that yields, so it is
        // constrained to a single truncating line rather than growing and displacing the rail.
        let longName = String(repeating: "very-long-name-", count: 10)
        let lineLimit = try GamblerScoreItem(
            poolGamblerScore: previewModel(username: longName),
            isCurrentUser: true
        )
        .inspect()
        .find(text: longName)
        .lineLimit()

        XCTAssertEqual(lineLimit, 1)
    }

    func testAccessibilityDescriptionExposesLocalizedRankAndMovement() throws {
        let label = try GamblerScoreItem(
            poolGamblerScore: previewModel(username: "neptune-player"),
            isCurrentUser: true
        )
        .inspect()
        .find(ViewType.Group.self)
        .accessibilityLabel()
        .string()

        XCTAssertTrue(label.contains("Rank 1"), "accessibility label was: \(label)")
        XCTAssertTrue(label.contains("up 1 places"), "accessibility label was: \(label)")
    }

    private func renderedTexts(username: String) throws -> [String] {
        try GamblerScoreItem(
            poolGamblerScore: previewModel(username: username),
            isCurrentUser: true
        )
        .inspect()
        .findAll(ViewType.Text.self)
        .map { try $0.string() }
    }

    private func previewModel(username: String) -> PoolGamblerScoreModel {
        // Mirrors the app's UsernamePreview projection: position 1, beforePosition 2 (an upward
        // movement of one place through the production rank rail), score 18.
        PoolGamblerScoreModel(
            poolId: "username-editor-preview-pool",
            poolName: "username-editor-preview",
            gamblerId: "preview-account",
            gamblerUsername: username,
            position: 1,
            beforePosition: 2,
            score: 18,
            gamblerCount: nil
        )
    }
}
