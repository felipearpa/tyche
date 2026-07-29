import SwiftUI
import Core
import Session

public struct AutoEmailAvatar: View {
    private let explicitEmail: String?
    @Environment(\.diResolver) private var diResolver: DIResolver
    @State private var resolvedEmail: String = ""
    @State private var resolvedAccountId: String = ""

    public init(email: String) { self.explicitEmail = email }
    public init() { self.explicitEmail = nil }

    public var body: some View {
        AccountAvatar(
            accountId: explicitEmail == nil ? resolvedAccountId : "",
            email: explicitEmail ?? resolvedEmail
        )
        .task {
            guard explicitEmail == nil else { return }
            guard let storage = diResolver.resolve(AccountStorage.self) else { return }
            let bundle = try? await storage.retrieve()
            resolvedEmail = bundle?.email ?? ""
            resolvedAccountId = bundle?.accountId ?? ""
        }
    }
}

public struct EmailAvatar: View {
    let email: String

    public init(email: String) {
        self.email = email
    }

    public var body: some View {
        InitialAvatar(
            identity: email.emailAvatarIdentity,
            colorKey: email
        )
    }
}

public extension View {
    func navigationEmailAvatar() -> some View {
        self
            .frame(width: navigationAvatarSize, height: navigationAvatarSize)
            .clipShape(Circle())
    }
}

private let navigationAvatarSize: CGFloat = 32

#Preview("Default") {
    EmailAvatar(email: "felipearpa@email.com")
        .frame(width: 64, height: 64)
        .clipShape(Circle())
}

#Preview("Fallback") {
    EmailAvatar(email: "@email.com")
        .frame(width: 64, height: 64)
        .clipShape(Circle())
}
