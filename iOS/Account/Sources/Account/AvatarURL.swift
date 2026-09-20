import Foundation

/// Avatar objects live at a deterministic key, so the URL is a pure function of the account id.
/// Swapping the host (e.g. to a CDN) is a one-constant change.
public enum AvatarURL {
    public static func of(accountId: String) -> URL? {
        guard !accountId.isEmpty else { return nil }
        return URL(string: "\(host)/avatars/\(accountId).jpg")
    }

    private static let host = "https://tyche-avatars.s3.us-east-2.amazonaws.com"
}
