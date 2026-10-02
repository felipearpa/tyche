import SwiftUI
import Testing
import ViewInspector
@testable import Account
import UI
@testable import Pool

/// `isPlaceholder` alone decides whether a leaderboard row is inert, mirroring the path
/// explored on the Android emulator (ARTEMIS) and the iOS 18.1 simulator: while the first
/// page loads, rows neither navigate nor request avatars; once real rows arrive, they open
/// the gambler's timeline and load avatars again.
@MainActor
struct GamblerScoreItemPlaceholderBehaviorTests {
    @Test
    func placeholderAvatarNeverRequestsOrDrawsTheFillerIdentity() throws {
        let row = placeholderItem(poolGamblerScorePlaceholderModel())

        let avatar = try row.inspect().find(AccountAvatar.self).actualView()
        #expect(avatar.isPlaceholder)
        #expect(!avatar.loadsPhoto)
        #expect(throws: (any Error).self) {
            try row.inspect().find(InitialAvatar.self)
        }
    }

    @Test
    func placeholderSuppressionFollowsTheFlagNotTheFillerIdentity() throws {
        // A real-looking model rendered as a placeholder is still inert.
        let row = placeholderItem(poolGamblerScoreDummyModel())

        #expect(!(try row.inspect().find(AccountAvatar.self).actualView().loadsPhoto))
        #expect(!(try rowContainer(row).allowsHitTesting()))
        #expect(try rowContainer(row).accessibilityHidden())
    }

    @Test
    func placeholderRowIgnoresTouchesAndHasNoActivationTarget() throws {
        #expect(!(try rowContainer(placeholderItem(poolGamblerScorePlaceholderModel())).allowsHitTesting()))

        // Initial and append loading render this row; it never wraps the item in the
        // gambler-detail button that loaded rows use.
        let appendRow = GamblerScorePlaceholderRow()
        #expect(try appendRow.inspect().findAll(ViewType.Button.self).isEmpty)
        #expect(try GamblerScorePlaceholderList().inspect().findAll(ViewType.Button.self).isEmpty)
    }

    @Test
    func placeholderRowIsHiddenFromAssistiveTechnology() throws {
        let row = placeholderItem(poolGamblerScorePlaceholderModel())

        #expect(try rowContainer(row).accessibilityHidden())
        #expect(row.accessibilityLabel.isEmpty)
    }

    @Test
    func loadedRowRestoresActionsAccessibilityAndAvatarLoading() throws {
        let model = poolGamblerScoreDummyModel()
        let loaded = GamblerScoreItem(poolGamblerScore: model, isCurrentUser: false)

        #expect(try rowContainer(loaded).allowsHitTesting())
        #expect(!(try rowContainer(loaded).accessibilityHidden()))
        #expect(loaded.accessibilityLabel.contains(model.gamblerUsername))

        let avatar = try loaded.inspect().find(AccountAvatar.self).actualView()
        #expect(!avatar.isPlaceholder)
        #expect(avatar.loadsPhoto)
        #expect(loaded.avatarAccountId == model.gamblerId)
    }

    private func placeholderItem(_ model: PoolGamblerScoreModel) -> GamblerScoreItem {
        GamblerScoreItem(poolGamblerScore: model, isCurrentUser: false, isPlaceholder: true)
    }

    /// The `Group` at the root of `body`, which carries the row's hit-testing and
    /// accessibility modifiers.
    private func rowContainer(_ item: GamblerScoreItem) throws -> InspectableView<ViewType.Group> {
        try item.inspect().find(ViewType.Group.self)
    }
}
