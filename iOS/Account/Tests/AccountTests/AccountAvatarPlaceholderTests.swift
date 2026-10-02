import SwiftUI
import Testing
@testable import Account

@Suite("AccountAvatar placeholder")
struct AccountAvatarPlaceholderTests {
    @Test("given a placeholder avatar then it never loads a photo, even for a real-looking account id")
    @MainActor
    func placeholderNeverLoads() {
        let avatar = AccountAvatar(
            accountId: "gambler",
            fallback: AccountAvatarFallback(identity: "Gambler"),
            isPlaceholder: true
        )

        #expect(!avatar.loadsPhoto)
    }

    @Test("given a real account then the avatar loads its photo by default")
    @MainActor
    func realAccountLoads() {
        let avatar = AccountAvatar(
            accountId: "gambler",
            fallback: AccountAvatarFallback(identity: "Gambler")
        )

        #expect(avatar.loadsPhoto)
    }

    @Test("given an account without an id then the avatar does not load a photo")
    @MainActor
    func missingIdDoesNotLoad() {
        let avatar = AccountAvatar(
            accountId: "",
            fallback: AccountAvatarFallback(identity: "Gambler")
        )

        #expect(!avatar.loadsPhoto)
    }
}
