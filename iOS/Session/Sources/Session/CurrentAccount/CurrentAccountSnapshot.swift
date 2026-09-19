import Foundation

/// The persisted form of the signed-in account: the bundle plus the moment the server
/// last confirmed it. `validatedAt == nil` means the snapshot has never been validated
/// against the authenticated current-account endpoint (for example, it was migrated from
/// the legacy raw-bundle format) and is therefore stale.
public struct CurrentAccountSnapshot: Codable, Equatable, Sendable {
    public let account: AccountBundle
    public let validatedAt: Date?

    public init(account: AccountBundle, validatedAt: Date?) {
        self.account = account
        self.validatedAt = validatedAt
    }
}

extension CurrentAccountSnapshot {
    /// Decodes the persisted account, accepting both the envelope format and the legacy
    /// raw `AccountBundle` written by previous releases. A legacy value is wrapped as
    /// never-validated so the next eligible trigger reconciles it, without signing the
    /// gambler out.
    static func decode(from raw: Data) throws -> CurrentAccountSnapshot {
        let decoder = JSONDecoder()
        if let snapshot = try? decoder.decode(CurrentAccountSnapshot.self, from: raw) {
            return snapshot
        }
        let legacyBundle = try decoder.decode(AccountBundle.self, from: raw)
        return CurrentAccountSnapshot(account: legacyBundle, validatedAt: nil)
    }
}
