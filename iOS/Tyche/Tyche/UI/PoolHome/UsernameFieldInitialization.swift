import Foundation

/// One-shot initialization of the username field for a screen presentation.
///
/// The first take populates the field with the stored username and reports the collapsed
/// caret position immediately after its final character (`0` when empty). Every later take
/// returns `nil`: recomposition, preview updates, validation, and `SaveState` transitions
/// never re-request focus or move a caret the gambler now owns.
///
/// At the iOS 16 deployment target SwiftUI has no explicit selection API, so the view applies
/// the caret by ordering: it assigns the draft first and requests focus second, and focusing a
/// populated field places the collapsed caret after the final character — exactly the reported
/// `caretIndex`. The index is grapheme-based, matching the field's visible content.
struct UsernameFieldInitialization {
    private var hasInitialized = false

    mutating func takeInitialization(
        initialUsername: String
    ) -> (draft: String, caretIndex: Int)? {
        guard !hasInitialized else { return nil }
        hasInitialized = true
        return (draft: initialUsername, caretIndex: initialUsername.count)
    }
}
