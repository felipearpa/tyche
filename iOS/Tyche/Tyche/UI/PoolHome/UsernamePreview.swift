import Foundation
import Pool

/// Builds the illustrative `PoolGamblerScoreModel` that backs the username editor's
/// live pool preview.
///
/// The projection is pure: it combines the signed-in account id and the current
/// username draft with fixed illustrative rank and score values. It never reads a
/// repository, never performs a network request, and never persists anything. Only
/// the account id and draft are real user data; rank and score are constant examples.
enum UsernamePreview {
    /// Fixed illustrative rank shown in the preview row.
    static let position = 1
    /// Fixed illustrative previous rank; the production rank calculation
    /// (`beforePosition - position`) renders an upward movement of one place.
    static let beforePosition = 2
    /// Fixed illustrative score shown in the preview row.
    static let score = 18
    /// Local-only pool identity for the preview row; never sent anywhere.
    static let poolId = "username-editor-preview-pool"
    static let poolName = "username-editor-preview"

    /// Projects the account id and draft username into the production leaderboard model.
    ///
    /// - Parameters:
    ///   - accountId: the signed-in account id, so the row resolves the current avatar.
    ///   - draft: the live username draft; trimmed of surrounding whitespace.
    ///   - placeholder: the localized text shown when the trimmed draft is empty.
    /// - Returns: a presentation-only model with fixed illustrative rank, score, and an
    ///   upward movement of one place rendered by the production rank rail.
    static func gamblerScore(
        accountId: String,
        draft: String,
        placeholder: String
    ) -> PoolGamblerScoreModel {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        return PoolGamblerScoreModel(
            poolId: poolId,
            poolName: poolName,
            gamblerId: accountId,
            gamblerUsername: trimmed.isEmpty ? placeholder : trimmed,
            position: position,
            beforePosition: beforePosition,
            score: score,
            gamblerCount: nil
        )
    }
}
