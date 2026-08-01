import SwiftUI
import Testing
import ViewInspector
import Account
import UI
@testable import Pool

/// Loading placeholders must be the production `GamblerScoreItem` populated with
/// `poolGamblerScorePlaceholderModel()` — never a separately maintained skeleton row —
/// so placeholder geometry and future row changes cannot drift from loaded rows.
/// Initial loading composes `GamblerScorePlaceholderList`, append loading constructs
/// `GamblerScorePlaceholderRow()`; both funnel into the same row type asserted here.
@MainActor
struct GamblerScorePlaceholderTests {
    @Test
    func initialLoadingListComposesTheSharedPlaceholderRow() throws {
        let rows = try GamblerScorePlaceholderList()
            .inspect()
            .findAll(GamblerScorePlaceholderRow.self)

        #expect(rows.count == 50)
    }

    @Test
    func placeholderRowRendersTheProductionRowAnatomy() throws {
        // Zero-arg construction is exactly what the append-loading slot renders.
        let row = GamblerScorePlaceholderRow()

        _ = try row.inspect().find(GamblerScoreItem.self)
        _ = try row.inspect().find(PostionIndicator.self)
        _ = try row.inspect().find(TrendIndicator.self)
        _ = try row.inspect().find(AccountAvatar.self)

        let texts = try row.inspect()
            .findAll(ViewType.Text.self)
            .map { try $0.string() }

        // The production slots are populated from the placeholder model:
        // rank tile "1" and upward movement "1", username, and score.
        #expect(texts.filter { $0 == "1" }.count == 2)
        #expect(texts.contains(String(repeating: "X", count: 15)))
        #expect(texts.contains("10"))
    }

    @Test
    func placeholderSuppressesAvatarRequestAndVoiceOver() throws {
        let item = GamblerScoreItem(
            poolGamblerScore: poolGamblerScorePlaceholderModel(),
            isCurrentUser: false,
            placeholderModifier: ShimmerModifier()
        )

        // The placeholder identity must never reach the avatar endpoint: the row
        // hands an empty account id to `AccountAvatar`, and an empty id can never
        // produce a request URL.
        #expect(item.avatarAccountId.isEmpty)
        #expect(AvatarURL.of(accountId: "") == nil)

        let label = try item.inspect()
            .find(ViewType.Group.self)
            .accessibilityLabel()
            .string()
        #expect(label.isEmpty)
    }

    @Test
    func placeholderKeepsProductionRowGeometryAndInheritedBackground() throws {
        let placeholder = GamblerScoreItem(
            poolGamblerScore: poolGamblerScorePlaceholderModel(),
            isCurrentUser: false,
            placeholderModifier: ShimmerModifier()
        )
        let loaded = GamblerScoreItem(
            poolGamblerScore: poolGamblerScoreDummyModel(),
            isCurrentUser: false
        )

        let placeholderFrame = try placeholder.inspect().find(ViewType.Group.self).flexFrame()
        let loadedFrame = try loaded.inspect().find(ViewType.Group.self).flexFrame()
        #expect(placeholderFrame.minHeight == loadedFrame.minHeight)
        #expect(placeholderFrame.maxWidth == loadedFrame.maxWidth)

        // Neither the placeholder nor a neutral loaded row paints its own canvas,
        // so both inherit the base background from the list's container; only the
        // signed-in gambler's row keeps its opaque highlight.
        #expect(placeholder.rowBackground == Color.clear)
        #expect(loaded.rowBackground == Color.clear)
        #expect(
            GamblerScoreItem(
                poolGamblerScore: poolGamblerScoreDummyModel(),
                isCurrentUser: true
            ).rowBackground == Color(sharedResource: .currentUserContainer)
        )
    }
}
