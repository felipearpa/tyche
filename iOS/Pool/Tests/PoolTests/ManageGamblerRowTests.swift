import SwiftUI
import Testing
import ViewInspector
import ViewingState
import DataPool
import UI
@testable import Pool

/// Member rows as the list builds them: the owner's row offers no removal route, and a row the
/// owner acted on follows its `GamblerRowHolder`.
@MainActor
struct ManageGamblerRowTests {
    private let owner = PoolMemberModel(
        gamblerId: "owner",
        gamblerUsername: "felipearpa",
        gamblerEmail: "felipe@example.com",
        isOwner: true
    )

    @Test
    func ownerRowHasNoSwipeAndNoRemoveAction() throws {
        let row = try memberRow(owner).inspect()

        #expect(throws: (any Error).self) {
            try row.find(SwipeToDismissBox<ManageGamblerSwipeBackground, ManageGamblerItem>.self)
        }
        #expect(removeActionLookup(in: row) == .absent)
    }

    @Test
    func otherRowsOfferTheRemoveAction() throws {
        let row = try memberRow(poolMemberModel("santiago")).inspect()

        #expect(removeActionLookup(in: row) == .present)
    }

    @Test
    func removingRowOffersNoRemoveAction() throws {
        let item = ManageGamblerItem(
            state: .mutating(poolMemberModel("santiago")),
            onRequestRemove: nil,
            onRetry: {},
            onDismissFailure: {}
        )

        #expect(removeActionLookup(in: try item.inspect()) == .absent)
    }

    @Test
    func removingRowIsDescribedAsRemoving() throws {
        let removing = ManageGamblerItem(
            state: .mutating(poolMemberModel("santiago")),
            onRequestRemove: nil,
            onRetry: {},
            onDismissFailure: {}
        )
        let idle = ManageGamblerItem(
            state: .idle(poolMemberModel("santiago")),
            onRequestRemove: {},
            onRetry: {},
            onDismissFailure: {}
        )

        #expect(try describesAsRemoving(removing.inspect()))
        #expect(!(try describesAsRemoving(idle.inspect())))
        #expect(String(localized: .removingGamblerAccessibilityValue) == "Removing")
    }

    @Test
    func holderStateChangeRedrawsTheRow() async throws {
        let repository = ScriptedPoolMemberRepository()
        let outcomes = IgnoredOutcomes()
        let member = poolMemberModel("santiago")
        let holder = GamblerRowHolder(
            member: member,
            poolId: testPoolId,
            removeGamblerUseCase: RemoveGamblerUseCase(poolMemberRepository: repository),
            outcomes: outcomes
        )
        let row = GamblerRowStateView(member: member, holder: holder, onRequestRemove: { _ in })

        _ = try row.inspect().find(text: member.gamblerEmail)
        #expect(throws: (any Error).self) { try row.inspect().find(ViewType.Button.self) }

        holder.remove()
        try #require(await eventually { repository.removals.pendingCount == 1 })
        repository.removals.answer(.failure(RemovalFailure()))
        #expect(await eventually { holder.removal.isFailure() })

        let failed = try row.inspect()
        #expect(throws: (any Error).self) { try failed.find(text: member.gamblerEmail) }
        let item = try failed.find(ManageGamblerItem.self).actualView()
        #expect(item.state.isFailure())
        #expect(try failed.findAll(ViewType.Button.self).count == 2)

        // Retry reaches the holder and starts the removal again without a confirmation.
        try failed.find(ViewType.Button.self).tap()
        #expect(holder.removal.isMutating())
        #expect(await eventually { repository.removals.calls == 2 })
        _ = try row.inspect().find(text: member.gamblerEmail)
    }

    @Test
    func dismissReturnsTheFailedRowToIdle() async throws {
        let repository = ScriptedPoolMemberRepository()
        let outcomes = IgnoredOutcomes()
        let member = poolMemberModel("santiago")
        let holder = GamblerRowHolder(
            member: member,
            poolId: testPoolId,
            removeGamblerUseCase: RemoveGamblerUseCase(poolMemberRepository: repository),
            outcomes: outcomes
        )
        let row = GamblerRowStateView(member: member, holder: holder, onRequestRemove: { _ in })
        holder.remove()
        try #require(await eventually { repository.removals.pendingCount == 1 })
        repository.removals.answer(.failure(RemovalFailure()))
        #expect(await eventually { holder.removal.isFailure() })

        try row.inspect().findAll(ViewType.Button.self)[1].tap()

        #expect(holder.removal.isIdle())
        #expect(repository.removals.calls == 1)
        _ = try row.inspect().find(text: member.gamblerEmail)
    }

    @Test
    func failedControlsNameTheGambler() throws {
        let item = ManageGamblerItem(
            state: .failure(value: poolMemberModel("santiago"), error: RemovalFailure()),
            onRequestRemove: {},
            onRetry: {},
            onDismissFailure: {}
        )

        let buttons = try item.inspect().findAll(ViewType.Button.self)
        #expect(buttons.count == 2)
        #expect(try buttons[0].accessibilityLabel().string() == "Retry removing @santiago")
        #expect(try buttons[1].accessibilityLabel().string() == "Dismiss, couldn’t remove @santiago")
    }

    private func memberRow(_ member: PoolMemberModel) -> ManageGamblerMemberRow {
        ManageGamblerMemberRow(member: member, holder: nil, isRemoved: false, onRequestRemove: { _ in })
    }

    /// Whether `root` sets the "Removing" VoiceOver value. Like the "Remove" action, this reads
    /// the modifier that applies it.
    private func describesAsRemoving<V>(_ root: InspectableView<V>) -> Bool {
        root.findAll(where: { (try? $0.modifier(RemovingAccessibilityValue.self)) != nil })
            .contains { (try? $0.modifier(RemovingAccessibilityValue.self).actualView().isRemoving) == true }
    }

    private enum RemoveActionLookup {
        case present
        case absent
    }

    /// Whether `root` offers VoiceOver's "Remove" action. ViewInspector cannot read accessibility
    /// actions on iOS 16 and later, so this reads the modifier that applies the action instead.
    private func removeActionLookup<V>(in root: InspectableView<V>) -> RemoveActionLookup {
        let modifiers = root.findAll(where: { (try? $0.modifier(RemoveAccessibilityAction.self)) != nil })
        let offersRemove = modifiers.contains { view in
            (try? view.modifier(RemoveAccessibilityAction.self).actualView().onRequestRemove) != nil
        }
        return offersRemove ? .present : .absent
    }
}

@MainActor
private final class IgnoredOutcomes: GamblerRowOutcomes {
    func removed(_ gamblerId: String) {}
    func failed(_ gamblerId: String) {}
    func failureCleared(_ gamblerId: String) {}
}
