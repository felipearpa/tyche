import Core
import Foundation
import LazyPaging
import SwiftUI
import Testing
import ViewInspector
@testable import UI

/// The shared full-list load error offers Retry beneath its message. Through the paging stacks'
/// default error content, Retry requests the list once more, and the refresh that pull to refresh
/// runs keeps working afterwards.
@MainActor
struct LazyPagingVStackErrorTests {
    @Test
    func retryIsALabelledButtonThatRunsItsActionOnce() throws {
        var retries = 0
        let error = LazyPagingVStackError(localizedError: UnknownLocalizedError(), retry: { retries += 1 })

        let button = try error.inspect().find(ViewType.Button.self)
        #expect(try button.labelView().text().string() == "Retry")

        try button.tap()
        #expect(retries == 1)
    }

    @Test
    func lazyPagingVStackDefaultRetryRequestsTheListOnceAndRefreshStillWorks() async throws {
        let source = FailingOnceSource()
        let items = source.makeItems()
        await items.refresh()
        #expect(items.loadState.refresh.isFailure)

        let stack = LazyPaging.LazyPagingVStack(
            lazyPagingItems: items,
            loadingContent: { EmptyView() },
            rowContent: { _ in EmptyView() }
        )
        try await tapRetryAndExpectOneMoreRequest(in: stack, items: items, source: source)
    }

    @Test
    func refreshableStackDefaultRetryRequestsTheListOnceAndRefreshStillWorks() async throws {
        let source = FailingOnceSource()
        let items = source.makeItems()
        await items.refresh()

        let stack = LazyPaging.RefreshableLazyPagingVStack(
            lazyPagingItems: items,
            loadingContent: { EmptyView() },
            rowContent: { _ in EmptyView() }
        )
        try await tapRetryAndExpectOneMoreRequest(in: stack, items: items, source: source)
    }

    @Test
    func refreshableStackWithAppendLoadingDefaultRetryRequestsTheListOnce() async throws {
        let source = FailingOnceSource()
        let items = source.makeItems()
        await items.refresh()

        let stack = LazyPaging.RefreshableLazyPagingVStack(
            lazyPagingItems: items,
            loadingContent: { EmptyView() },
            appendLoadingContent: { EmptyView() },
            rowContent: { _ in EmptyView() }
        )
        try await tapRetryAndExpectOneMoreRequest(in: stack, items: items, source: source)
    }

    @Test
    func refreshableStackWithAllPageContentDefaultRetryRequestsTheListOnce() async throws {
        let source = FailingOnceSource()
        let items = source.makeItems()
        await items.refresh()

        let stack = LazyPaging.RefreshableLazyPagingVStack(
            lazyPagingItems: items,
            loadingContent: { EmptyView() },
            prependLoadingContent: { EmptyView() },
            appendLoadingContent: { EmptyView() },
            prependErrorContent: { _ in EmptyView() },
            appendErrorContent: { _ in EmptyView() },
            rowContent: { _ in EmptyView() }
        )
        try await tapRetryAndExpectOneMoreRequest(in: stack, items: items, source: source)
    }

    /// A pull to refresh over loaded rows lets the visible rows ask for the next page while the
    /// refresh runs; that append is skipped, but `LazyPagingItems` records it as the load to
    /// retry. When the refresh then fails, Retry must still request the list again.
    @Test
    func retryAfterAFailedRefreshOverLoadedRowsRequestsTheListAgain() async throws {
        let source = GatedRefreshSource()
        let items = source.makeItems()
        await items.refresh()
        #expect(items.itemCount == 2)

        let refresh = Task { await items.refresh() }
        try await waitUntil { source.isHoldingRequest }
        await items.onRowAccess(index: 1)
        source.releaseHeldRequest(with: URLError(.notConnectedToInternet))
        await refresh.value
        #expect(items.loadState.refresh.isFailure)
        #expect(source.requestCount == 2)

        let stack = LazyPaging.RefreshableLazyPagingVStack(
            lazyPagingItems: items,
            loadingContent: { EmptyView() },
            rowContent: { _ in EmptyView() }
        )
        try stack.inspect().find(LazyPagingVStackError<EmptyView>.self).find(ViewType.Button.self).tap()
        try await waitUntil { source.requestCount == 3 && !items.loadState.refresh.isLoading }

        #expect(!items.loadState.refresh.isFailure)
        #expect(items.itemCount == 2)
    }

    /// Taps the error's Retry, waits for the rows, then runs the refresh that pull to refresh
    /// triggers and expects exactly one request for each.
    private func tapRetryAndExpectOneMoreRequest<Stack: View>(
        in stack: Stack,
        items: LazyPaging.LazyPagingItems<String, PagingRow>,
        source: FailingOnceSource
    ) async throws {
        #expect(source.requestCount == 1)

        try stack.inspect().find(LazyPagingVStackError<EmptyView>.self).find(ViewType.Button.self).tap()
        try await waitUntil { items.itemCount == 2 }
        #expect(source.requestCount == 2)

        await items.refresh()
        #expect(source.requestCount == 3)
        #expect(items.itemCount == 2)
        #expect(!items.loadState.refresh.isFailure)
    }

    /// Budgets polls, not wall time: a busy CI main actor can stall this task far past any deadline.
    private func waitUntil(polls: Int = 500, _ condition: () -> Bool) async throws {
        var remaining = polls
        while !condition() {
            guard remaining > 0 else { throw ConditionTimeout() }
            remaining -= 1
            try await Task.sleep(for: .milliseconds(10))
        }
    }
}

private struct PagingRow: Identifiable, Hashable, Sendable, Codable {
    let id: Int
}

private struct ConditionTimeout: Error {}

/// A list whose first request fails and whose later requests return two rows.
@MainActor
private final class FailingOnceSource {
    private(set) var requestCount = 0

    func makeItems() -> LazyPaging.LazyPagingItems<String, PagingRow> {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: PagingConfig(pageSize: 25, prefetchDistance: 5),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PagingRow>(pagingQuery: { _ in await self.nextPage() })
                }
            )
        )
    }

    private func nextPage() -> Result<CursorPage<PagingRow>, Error> {
        requestCount += 1
        guard requestCount > 1 else { return .failure(URLError(.notConnectedToInternet)) }
        return .success(CursorPage(items: [PagingRow(id: 1), PagingRow(id: 2)], next: nil))
    }
}

/// The first request returns two rows and a next-page cursor. The second is held until the test
/// releases it with an error. Later requests return the two rows again.
@MainActor
private final class GatedRefreshSource {
    private(set) var requestCount = 0
    private var held: CheckedContinuation<Result<CursorPage<PagingRow>, Error>, Never>?

    var isHoldingRequest: Bool { held != nil }

    func makeItems() -> LazyPaging.LazyPagingItems<String, PagingRow> {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: PagingConfig(pageSize: 2, prefetchDistance: 1),
                pagingSourceFactory: {
                    LazyPagingCursorSource<PagingRow>(pagingQuery: { _ in await self.nextPage() })
                }
            )
        )
    }

    func releaseHeldRequest(with error: Error) {
        held?.resume(returning: .failure(error))
        held = nil
    }

    private func nextPage() async -> Result<CursorPage<PagingRow>, Error> {
        requestCount += 1
        let rows = CursorPage(items: [PagingRow(id: 1), PagingRow(id: 2)], next: "page-2")
        guard requestCount == 2 else { return .success(rows) }
        return await withCheckedContinuation { held = $0 }
    }
}
