import Foundation
import Testing
@testable import Tyche

@Suite("UsernameDraftRules")
struct UsernameDraftRulesTests {
    @Test("given a draft within the limit when clamped then it is returned unchanged")
    func clampWithinLimit() {
        let draft = String(repeating: "a", count: 100)
        #expect(UsernameDraftRules.clamp(draft) == draft)
    }

    @Test("given a draft over the limit when clamped then it is cut to 100 graphemes")
    func clampOverLimit() {
        let draft = String(repeating: "a", count: 150)
        let clamped = UsernameDraftRules.clamp(draft)
        #expect(clamped.count == 100)
    }

    @Test("given multi-scalar graphemes over the limit when clamped then whole clusters are preserved")
    func clampCountsGraphemeClusters() {
        // Family emoji is a single grapheme cluster made of several unicode scalars.
        let draft = String(repeating: "👨‍👩‍👧‍👦", count: 120)
        let clamped = UsernameDraftRules.clamp(draft)
        #expect(clamped.count == 100)
    }

    @Test("given a draft when counted then the count matches its grapheme clusters")
    func graphemeCount() {
        #expect(UsernameDraftRules.graphemeCount("abc") == 3)
        #expect(UsernameDraftRules.graphemeCount("👨‍👩‍👧‍👦") == 1)
    }

    @Test("given a whitespace-only draft when checked then it is empty")
    func emptyWhenWhitespace() {
        #expect(UsernameDraftRules.isEmpty("   \n ") == true)
        #expect(UsernameDraftRules.isEmpty(" a ") == false)
    }

    @Test("given an empty trimmed draft then save is not eligible")
    func cannotSaveWhenEmpty() {
        #expect(UsernameDraftRules.canSave(draft: "   ", initial: "old", isSaving: false) == false)
    }

    @Test("given a draft equal to the initial username then save is not eligible")
    func cannotSaveWhenUnchanged() {
        #expect(UsernameDraftRules.canSave(draft: "  neptune  ", initial: "neptune", isSaving: false) == false)
    }

    @Test("given an in-flight save then save is not eligible even when changed")
    func cannotSaveWhileSaving() {
        #expect(UsernameDraftRules.canSave(draft: "changed", initial: "old", isSaving: true) == false)
    }

    @Test("given a changed non-empty draft and no save in flight then save is eligible")
    func canSaveWhenChanged() {
        #expect(UsernameDraftRules.canSave(draft: "  changed ", initial: "old", isSaving: false) == true)
    }
}
