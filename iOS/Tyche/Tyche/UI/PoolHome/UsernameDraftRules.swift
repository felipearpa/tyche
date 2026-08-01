import Foundation

/// Pure rules that govern the username draft: the grapheme limit, the visible counter,
/// and when the "Save username" action is eligible. Kept free of view and view-model
/// state so the behavior is unit-testable in isolation.
enum UsernameDraftRules {
    static let maxGraphemes = 100

    /// Number of grapheme clusters in the draft, matching what the counter displays.
    static func graphemeCount(_ value: String) -> Int {
        value.count
    }

    /// Clamps the draft to at most `max` grapheme clusters, preserving the original
    /// value when it already fits.
    static func clamp(_ value: String, max: Int = maxGraphemes) -> String {
        let graphemes = Array(value)
        if graphemes.count <= max { return value }
        return String(graphemes.prefix(max))
    }

    /// The trimmed draft, with surrounding whitespace and newlines removed.
    static func trimmed(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Whether the trimmed draft is empty.
    static func isEmpty(_ value: String) -> Bool {
        trimmed(value).isEmpty
    }

    /// "Save username" is eligible only when the trimmed draft is non-empty, differs
    /// from the initial trimmed username, and no save is in progress.
    static func canSave(draft: String, initial: String, isSaving: Bool) -> Bool {
        let trimmedDraft = trimmed(draft)
        return !trimmedDraft.isEmpty
            && trimmedDraft != trimmed(initial)
            && !isSaving
    }
}
