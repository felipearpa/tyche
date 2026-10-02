import SwiftUI
import Testing
import ViewInspector
import ViewingState
@testable import Account
import UI
@testable import Pool

/// `isPlaceholder` alone decides whether My pools rows, pool templates, and Manage gamblers rows
/// are inert, mirroring the paths explored on the iOS 18.1 simulator: while those lists load,
/// tapping a placeholder row or its invite control does nothing and the rows are skipped by
/// assistive technology; once real rows arrive, they open, invite, and announce again.
@MainActor
struct PoolPlaceholderSurfacesTests {
    // MARK: My pools

    @Test
    func placeholderPoolRowIgnoresTouchesAndIsHiddenEvenWithARealModel() throws {
        let row = PoolScoreItem(
            poolGamblerScore: poolGamblerScoreDummyModel(),
            onOpen: {},
            onJoin: {},
            isPlaceholder: true
        )

        #expect(!(try container(row).allowsHitTesting()))
        #expect(try container(row).accessibilityHidden())
        #expect(row.accessibilityLabel.isEmpty)
    }

    @Test
    func placeholderPoolRowNeverRunsItsOpenOrInviteActions() throws {
        var opened = 0
        var invited = 0
        let row = PoolScoreItem(
            poolGamblerScore: poolGamblerScorePlaceholderModel(),
            onOpen: { opened += 1 },
            onJoin: { invited += 1 },
            isPlaceholder: true
        )

        // The invite control keeps its production geometry but sits under a container that
        // ignores touches and is hidden from assistive technology.
        _ = try row.inspect().find(ViewType.Button.self)
        #expect(!(try container(row).allowsHitTesting()))
        #expect(opened == 0 && invited == 0)
    }

    @Test
    func loadedPoolRowRestoresInviteActionAndAnnouncement() throws {
        var invited = 0
        let model = poolGamblerScoreDummyModel()
        let row = PoolScoreItem(poolGamblerScore: model, onOpen: {}, onJoin: { invited += 1 })

        try row.inspect().find(ViewType.Button.self).tap()
        #expect(invited == 1)
        #expect(row.accessibilityLabel.hasPrefix(model.poolName))
    }

    // MARK: Pool templates

    @Test
    func placeholderTemplateCardIsInertAndHidden() throws {
        let card = PoolFromLayoutCreatorItem(
            poolLayout: poolLayoutFakeModel(),
            isSelected: false,
            isPlaceholder: true
        )

        #expect(!(try container(card).allowsHitTesting()))
        #expect(try container(card).accessibilityHidden())
    }

    @Test
    func loadedTemplateCardAcceptsTouchesAndIsExposed() throws {
        let card = PoolFromLayoutCreatorItem(poolLayout: poolLayoutDummyModel(), isSelected: false)

        #expect(try container(card).allowsHitTesting())
        #expect(!(try container(card).accessibilityHidden()))
    }

    // MARK: Manage gamblers

    @Test
    func loadingListRendersEightProductionRowsWithoutAttenuationOrActions() throws {
        let list = try ManageGamblerPlaceholderList().inspect()

        let items = try list.findAll(ManageGamblerItem.self)
        #expect(items.count == 8)
        #expect(try items.allSatisfy { try $0.actualView().isPlaceholder })
        // No swipe-to-remove container, remove button, or per-row opacity.
        #expect(try list.findAll(ViewType.Button.self).isEmpty)
        #expect(try list.findAll(ManageGamblerPlaceholderRow.self).allSatisfy {
            (try? $0.opacity()) == nil
        })
    }

    @Test
    func placeholderMemberDrawsNoFillerIdentityAndRequestsNoPhoto() throws {
        let row = try ManageGamblerPlaceholderRow().inspect()

        let avatar = try row.find(AccountAvatar.self).actualView()
        #expect(avatar.isPlaceholder)
        #expect(!avatar.loadsPhoto)
        #expect(throws: (any Error).self) { try row.find(EmailAvatar.self) }
        #expect(throws: (any Error).self) { try row.find(InitialAvatar.self) }

        let item = try row.find(ManageGamblerItem.self)
        #expect(!(try item.find(ViewType.Group.self).allowsHitTesting()))
        #expect(try item.find(ViewType.Group.self).accessibilityHidden())
    }

    @Test
    func loadedMemberRestoresItsEmailAvatar() throws {
        let item = ManageGamblerItem(
            state: .idle(
                PoolMemberModel(
                    gamblerId: "1",
                    gamblerUsername: "danielsanto",
                    gamblerEmail: "daniel@example.com"
                )
            )
        )

        _ = try item.inspect().find(EmailAvatar.self)
        #expect(throws: (any Error).self) { try item.inspect().find(AccountAvatar.self) }
    }

    /// The `Group` at the root of each component's `body`, which carries its hit-testing and
    /// accessibility modifiers.
    private func container<V: View>(_ view: V) throws -> InspectableView<ViewType.Group> {
        try view.inspect().find(ViewType.Group.self)
    }
}
