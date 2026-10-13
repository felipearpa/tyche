import Core
import Foundation
import LazyPaging
import SwiftUI
import Testing
@testable import UI

/// The append error's Retry repeats the failed append. Rows of `AppendPagingVStack` arm only the
/// append, so a row near the top appearing after the failure (a short list) cannot make Retry
/// repeat a prepend instead, as rows of `LazyPagingVStack` do through `onRowAccess`.
@MainActor
struct AppendPagingVStackTests {
    @Test
    func retryAfterRowsAppearRepeatsTheFailedAppend() async throws {
        let source = ScriptedSource()
        let items = makeItems(source)
        await items.refresh()

        // A one-item list: its only row is both the last row and a top row.
        await AppendPagingVStack<String, Item, EmptyView, EmptyView, EmptyView, EmptyView, EmptyView, EmptyView>
            .rowDidAppear(items, at: 0)
        #expect(items.loadState.append.isFailure)

        source.failsAppend = false
        await AppendPagingVStack<String, Item, EmptyView, EmptyView, EmptyView, EmptyView, EmptyView, EmptyView>
            .rowDidAppear(items, at: 0)
        await items.retry()

        #expect(source.requests == [nil, "page-2", "page-2"])
        #expect(items.loadedItems.map(\.id) == ["a", "b"])
    }

    @Test
    func theLibraryRowAccessMakesRetryRepeatAPrependInstead() async throws {
        // Documents why the app's lists use `AppendPagingVStack`.
        let source = ScriptedSource()
        let items = makeItems(source)
        await items.refresh()

        await items.onRowAccess(index: 0)
        source.failsAppend = false
        await items.onRowAccess(index: 0)
        await items.retry()

        #expect(source.requests == [nil, "page-2"])
        #expect(items.loadState.append.isFailure)
    }

    private func makeItems(_ source: ScriptedSource) -> LazyPaging.LazyPagingItems<String, Item> {
        let pagingSource = LazyPagingCursorSource<Item>(pagingQuery: { next in await source.page(after: next) })
        return LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 20, prefetchDistance: 3, enablePlaceholders: false),
                pagingSourceFactory: { pagingSource }
            )
        )
    }
}

struct Item: Identifiable, Hashable, Sendable, Codable {
    let id: String
}

@MainActor
private final class ScriptedSource {
    var failsAppend = true
    private(set) var requests: [String?] = []

    func page(after next: String?) -> Result<CursorPage<Item>, Error> {
        requests.append(next)
        guard next != nil else { return .success(CursorPage(items: [Item(id: "a")], next: "page-2")) }
        if failsAppend { return .failure(URLError(.timedOut)) }
        return .success(CursorPage(items: [Item(id: "b")], next: nil))
    }
}
