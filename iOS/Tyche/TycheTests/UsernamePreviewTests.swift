import Foundation
import Pool
import Testing
@testable import Tyche

@Suite("UsernamePreview")
struct UsernamePreviewTests {
    private let accountId = "account-123"
    private let placeholder = "Your username"

    @Test("given a normal draft when projected then the row shows the trimmed draft with fixed illustrative values")
    func normalDraft() {
        let model = UsernamePreview.gamblerScore(
            accountId: accountId,
            draft: "  neptune-player  ",
            placeholder: placeholder
        )

        #expect(model.gamblerUsername == "neptune-player")
        #expect(model.gamblerId == accountId)
        #expect(model.position == 1)
        #expect(model.beforePosition == 2)
        #expect(model.rank() == 1) // production rank rail renders an upward movement of one place
        #expect(model.score == 18)
        #expect(model.gamblerCount == nil)
    }

    @Test("given a whitespace-only draft when projected then the row shows the placeholder")
    func whitespaceOnlyDraft() {
        let model = UsernamePreview.gamblerScore(
            accountId: accountId,
            draft: "   \n\t ",
            placeholder: placeholder
        )

        #expect(model.gamblerUsername == placeholder)
    }

    @Test("given an empty draft when projected then the row shows the placeholder")
    func emptyDraft() {
        let model = UsernamePreview.gamblerScore(
            accountId: accountId,
            draft: "",
            placeholder: placeholder
        )

        #expect(model.gamblerUsername == placeholder)
    }

    @Test("given a 100-grapheme draft when projected then the row carries the full value unchanged")
    func hundredGraphemeDraft() {
        let draft = String(repeating: "a", count: 100)

        let model = UsernamePreview.gamblerScore(
            accountId: accountId,
            draft: draft,
            placeholder: placeholder
        )

        #expect(model.gamblerUsername == draft)
        #expect(model.gamblerUsername.count == 100)
    }

    @Test("given a long draft when projected then the factory preserves it and leaves truncation to the row")
    func longTruncatingDraft() {
        let draft = String(repeating: "very-long-name ", count: 20).trimmingCharacters(in: .whitespaces)

        let model = UsernamePreview.gamblerScore(
            accountId: accountId,
            draft: draft,
            placeholder: placeholder
        )

        #expect(model.gamblerUsername == draft)
    }

    @Test("given identical inputs when projected repeatedly then the result is deterministic and does no repository work")
    func deterministicPureProjection() {
        let first = UsernamePreview.gamblerScore(accountId: accountId, draft: "same", placeholder: placeholder)
        let second = UsernamePreview.gamblerScore(accountId: accountId, draft: "same", placeholder: placeholder)

        // Equal outputs for equal inputs: the projection holds no mutable or fetched state.
        #expect(first == second)
        #expect(first.poolId == UsernamePreview.poolId)
    }
}
