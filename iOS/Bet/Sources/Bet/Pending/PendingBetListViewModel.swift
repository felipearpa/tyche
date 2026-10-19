import Foundation
import Combine
import Core
import UI
import LazyPaging
import DataBet

/// Which loaded matches the Bets tab shows.
enum PendingBetFilter: CaseIterable {
    /// Every loaded match.
    case all
    /// Open matches without a saved prediction, incomplete drafts included.
    case noBet
    /// Matches with a saved prediction, locked ones included.
    case placed
}

/// How many loaded matches each filter shows.
struct PendingBetFilterCounts: Equatable {
    let all: Int
    let noBet: Int
    let placed: Int
}

/// The visible matches of one kickoff day, in feed order.
struct PendingBetDaySection: Identifiable, Equatable {
    /// The start of the kickoff day in the current calendar.
    let day: Date
    let matches: [PoolGamblerBetModel]

    var id: Date { day }
}

@MainActor
public final class PendingBetListViewModel: ObservableObject {
    private let betUseCase: BetUseCase
    private let clock: any Clock<Duration>
    private var pagingSource: LazyPagingCursorSource<PoolGamblerBetModel>!
    private var loadedItemsSubscription: AnyCancellable?
    private var searchText: String? = nil

    /// One row per loaded match, keyed by match ID, kept for the screen's lifetime so drafts and
    /// in-flight saves survive paging updates and filtering. Not published: rows are created as
    /// pages land, and their changes reach the screen through `PendingBetRowOutcomes`.
    private var rowHolders: [String: PendingBetRowHolder] = [:]

    @Published var filter: PendingBetFilter = .all

    lazy var lazyPager: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel> = {
        LazyPaging.LazyPagingItems(
            pager: Pager(
                config: LazyPaging.PagingConfig(pageSize: 50, prefetchDistance: 5, enablePlaceholders: false),
                pagingSourceFactory: { [pagingSource = self.pagingSource!] in pagingSource }
            )
        )
    }()

    public convenience init(
        getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase,
        betUseCase: BetUseCase,
        gamblerId: String,
        poolId: String
    ) {
        self.init(
            getPoolGamblerBetsUseCase: getPoolGamblerBetsUseCase,
            betUseCase: betUseCase,
            gamblerId: gamblerId,
            poolId: poolId,
            clock: ContinuousClock()
        )
    }

    /// `clock` times the rows' save debounce.
    init(
        getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase,
        betUseCase: BetUseCase,
        gamblerId: String,
        poolId: String,
        clock: any Clock<Duration>
    ) {
        self.betUseCase = betUseCase
        self.clock = clock

        self.pagingSource = LazyPagingCursorSource<PoolGamblerBetModel>(
            pagingQuery: { next in
                await getPoolGamblerBetsUseCase.execute(
                    poolId: poolId,
                    gamblerId: gamblerId,
                    next: next,
                    searchText: nil
                )
                .map { page in page.map { poolGamblerBet in poolGamblerBet.toPoolGamblerBetModel() }}
                .mapError { error in error.toLocalizedError() }
            }
        )

        // `$loadedItems` emits before the pager stores the items, so every loaded match has its
        // row before the list renders it.
        loadedItemsSubscription = lazyPager.$loadedItems.sink { [weak self] items in
            self?.loadedItemsWillChange(to: items)
        }
    }

    var counts: PendingBetFilterCounts {
        let rows = loadedRows
        return PendingBetFilterCounts(
            all: rows.count,
            noBet: rows.filter { Self.matches($0, .noBet) }.count,
            placed: rows.filter { Self.matches($0, .placed) }.count
        )
    }

    /// The loaded matches the filter keeps, grouped under their kickoff days.
    var visibleSections: [PendingBetDaySection] {
        let calendar = Calendar.current
        return loadedRows
            .filter { Self.matches($0, filter) }
            .map(\.match)
            .reduce(into: [PendingBetDaySection]()) { sections, match in
                let day = calendar.startOfDay(for: match.matchDateTime)
                guard let last = sections.last, last.day == day else {
                    sections.append(PendingBetDaySection(day: day, matches: [match]))
                    return
                }
                sections[sections.count - 1] = PendingBetDaySection(day: day, matches: last.matches + [match])
            }
    }

    /// Whether later pages may still add matches, so the counts are lower bounds.
    var areCountsPartial: Bool {
        !lazyPager.loadState.append.endOfPaginationReached
    }

    /// Matches are loaded and every page has arrived, but the filter keeps none of them. While a
    /// later page may still hold a match the filter keeps, the list shows the append placeholder
    /// instead; the page trigger after the last loaded slot keeps paging while the filter hides
    /// every card.
    var isFilteredEmpty: Bool {
        lazyPager.loadState.append.endOfPaginationReached
            && !lazyPager.loadedItems.isEmpty
            && !loadedRows.contains { Self.matches($0, filter) }
    }

    /// Whether the filter keeps a loaded match.
    func isVisible(_ match: PoolGamblerBetModel) -> Bool {
        Self.matches(rowHolder(for: match), filter)
    }

    /// The row of a loaded match, created on first use.
    func rowHolder(for match: PoolGamblerBetModel) -> PendingBetRowHolder {
        if let row = rowHolders[match.matchId] {
            return row
        }
        let row = PendingBetRowHolder(match: match, betUseCase: betUseCase, outcomes: self, clock: clock)
        rowHolders[match.matchId] = row
        return row
    }

    func search(_ text: String) {
        searchText = text
        pagingSource.invalidate()
    }

    func refresh() {
        pagingSource.invalidate()
    }

    private var loadedRows: [PendingBetRowHolder] {
        lazyPager.loadedItems.map(rowHolder(for:))
    }

    private func loadedItemsWillChange(to items: [PoolGamblerBetModel]) {
        for match in items {
            rowHolder(for: match).bind(match)
        }
    }

    private static func matches(_ row: PendingBetRowHolder, _ filter: PendingBetFilter) -> Bool {
        switch filter {
        case .all: true
        case .noBet: row.isEditable && row.saved == nil
        case .placed: row.saved != nil
        }
    }
}

/// The list derives its counts and visible sections from the rows whenever it publishes, so each
/// reported outcome republishes it: a save or a lock moves the row between filters.
extension PendingBetListViewModel: PendingBetRowOutcomes {
    func saved(_ matchId: String) {
        rowChanged()
    }

    func failed(_ matchId: String) {
        rowChanged()
    }

    func failureCleared(_ matchId: String) {
        rowChanged()
    }

    func locked(_ matchId: String) {
        rowChanged()
    }

    private func rowChanged() {
        objectWillChange.send()
    }
}

private extension Error {
    func toLocalizedError() -> Error {
        if let networkError = self as? NetworkError {
            return networkError.toNetworkLocalizedError()
        }

        return UnknownLocalizedError()
    }
}
