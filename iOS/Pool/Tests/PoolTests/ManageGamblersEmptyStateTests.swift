import Testing
import LazyPaging
@testable import Pool

/// When the list shows "No other gamblers yet": only once every member has loaded and nobody but
/// the owner is left, never while a page is loading or after a load error.
struct ManageGamblersEmptyStateTests {
    private let owner = PoolMemberModel(
        gamblerId: "owner",
        gamblerUsername: "felipearpa",
        gamblerEmail: "felipe@example.com",
        isOwner: true
    )
    private let loaded = LoadState.notLoading(endOfPaginationReached: false)
    private let endOfPagination = LoadState.notLoading(endOfPaginationReached: true)

    @Test
    func firstPageLoadErrorShowsNoEmptyState() {
        #expect(!showsEmptyState(refresh: .failure(LoadFailure()), append: endOfPagination, members: []))
        #expect(!showsEmptyState(refresh: .failure(LoadFailure()), append: endOfPagination, members: [owner]))
    }

    @Test
    func firstPageLoadingShowsNoEmptyState() {
        #expect(!showsEmptyState(refresh: .loading, append: endOfPagination, members: []))
        #expect(!showsEmptyState(refresh: .loading, append: endOfPagination, members: [owner]))
    }

    @Test
    func nextPageLoadingOrFailedShowsNoEmptyState() {
        #expect(!showsEmptyState(refresh: loaded, append: .loading, members: [owner]))
        #expect(!showsEmptyState(refresh: loaded, append: .failure(LoadFailure()), members: [owner]))
    }

    @Test
    func morePagesToLoadShowsNoEmptyState() {
        #expect(!showsEmptyState(refresh: loaded, append: loaded, members: [owner]))
    }

    @Test
    func onlyTheOwnerAtEndOfPaginationShowsEmptyState() {
        #expect(showsEmptyState(refresh: loaded, append: endOfPagination, members: [owner]))
    }

    @Test
    func ownerWithOtherGamblersShowsNoEmptyState() {
        let members = [owner, poolMemberModel("santiago"), poolMemberModel("valentina")]

        #expect(!showsEmptyState(refresh: loaded, append: endOfPagination, members: members))
    }

    @Test
    func ownerWithSomeOthersRemovedShowsNoEmptyState() {
        let members = [owner, poolMemberModel("santiago"), poolMemberModel("valentina")]

        #expect(!showsEmptyState(refresh: loaded, append: endOfPagination, members: members, removed: ["santiago"]))
    }

    @Test
    func ownerWithAllOthersRemovedShowsEmptyState() {
        let members = [owner, poolMemberModel("santiago"), poolMemberModel("valentina")]

        #expect(showsEmptyState(
            refresh: loaded,
            append: endOfPagination,
            members: members,
            removed: ["santiago", "valentina"]
        ))
    }

    @Test
    func noLoadedMembersShowsNoEmptyState() {
        #expect(!showsEmptyState(refresh: loaded, append: endOfPagination, members: []))
    }

    /// Mirrors the screen's wiring: a member counts as removed when its ID is in the view model's
    /// `removedGamblerIds`.
    private func showsEmptyState(
        refresh: LoadState,
        append: LoadState,
        members: [PoolMemberModel],
        removed: Set<String> = []
    ) -> Bool {
        membersHoldOnlyTheOwner(
            refresh: refresh,
            append: append,
            members: members,
            isRemoved: { removed.contains($0.gamblerId) }
        )
    }
}

private struct LoadFailure: Error {}
