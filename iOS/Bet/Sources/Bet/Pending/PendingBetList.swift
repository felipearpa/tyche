import SwiftUI
import UIKit
import UI
import Core
import LazyPaging
import DataBet

/// The Bets tab's list: the filter row, then the loaded matches the filter keeps as cards under
/// sticky kickoff-date headers. Cards the filter hides keep their row holders, so drafts and
/// in-flight saves survive, but draw no view, so a long list holds views only for the cards it
/// shows. A trigger after the last loaded slot keeps the pager loading pages behind hidden cards.
/// A filter that keeps no loaded match shows its own message below the filter row once every page
/// has loaded.
struct PendingBetList: View {
    @ObservedObject var viewModel: PendingBetListViewModel
    let onMatchOpen: MatchOpenHandler?

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// The card whose score box holds focus, kept above the keyboard.
    @State private var focusedMatch: PoolGamblerBetModelId?
    /// Counts height changes of the focused card, such as the failure banner appearing, each of
    /// which scrolls the card above the keyboard again.
    @State private var focusedCardResizes = 0

    init(
        viewModel: PendingBetListViewModel,
        onMatchOpen: MatchOpenHandler? = nil
    ) {
        self.viewModel = viewModel
        self.onMatchOpen = onMatchOpen
    }

    var body: some View {
        let _ = Self._printChangesIfDebug()

        let lazyPagingItems = viewModel.lazyPager
        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        ScrollViewReader { proxy in
            RefreshableLazyPagingVStack(
                lazyPagingItems: lazyPagingItems,
                // The spacing goes inside the scroll view: an outer padding would keep it off the
                // bars' safe-area edges, so rows would stop short of them instead of scrolling beneath.
                contentInsets: contentInsets,
                pinnedViews: [.sectionHeaders],
                loadingContent: { PendingBetPlaceholderList() },
                appendLoadingContent: { PendingBetPlaceholderRow() },
            ) { index in
                if index == 0 {
                    Section {
                        filterRow
                        if viewModel.isFilteredEmpty, let message = viewModel.filter.emptyMessage {
                            PendingBetFilterEmptyMessage(filter: viewModel.filter, message: message)
                        }
                    }
                }
                row(at: index, in: lazyPagingItems)
                if index == lazyPagingItems.itemCount - 1 {
                    PendingBetPageTrigger(lazyPagingItems: lazyPagingItems, lastIndex: index)
                }
            }
            .scrollDismissesKeyboard(.immediately)
            .keepsBottomAnchoredOnSizeChanges()
            .settlesAtTopEdge()
            .fullListStateViewport(contentInsets: contentInsets)
            .onChange(of: focusedMatch) { match in reveal(match, with: proxy) }
            .onChange(of: focusedCardResizes) { _ in reveal(focusedMatch, with: proxy) }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidShowNotification)) { _ in
                reveal(focusedMatch, with: proxy)
            }
            .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
                focusedMatch = nil
            }
        }
    }

    /// Scrolls the focused card fully above the keyboard, both boxes and their steppers. Run when
    /// focus enters a card, again once the keyboard has settled, since the first scroll can
    /// happen before the keyboard's inset applies, and whenever the focused card's height changes,
    /// so rows appearing below its scores stay above the keyboard too.
    private func reveal(_ match: PoolGamblerBetModelId?, with proxy: ScrollViewProxy) {
        guard let match else { return }
        withAnimation(reduceMotion ? nil : PendingBetStyle.animation) {
            proxy.scrollTo(match, anchor: .bottom)
        }
    }

    private var filterRow: some View {
        PendingBetFilterRow(
            selected: viewModel.filter,
            counts: viewModel.counts,
            isPartial: viewModel.areCountsPartial
        ) { filter in
            withAnimation(reduceMotion ? nil : PendingBetStyle.animation) {
                viewModel.filter = filter
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, boxSpacing.large)
        .padding(.bottom, boxSpacing.small)
    }

    /// The first loaded match of a kickoff day draws that day's whole section: its header and
    /// every visible card of the day, so the pinned header leaves with the day's last card. The
    /// day's other slots, and days the filter hides entirely, draw nothing, so the stack holds
    /// views only for visible cards; `PendingBetPageTrigger` keeps paging going past them.
    @ViewBuilder
    private func row(
        at index: Int,
        in lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    ) -> some View {
        if lazyPagingItems.peek(at: index) == nil {
            Section {
                PendingBetPlaceholderRow()
            }
        } else if let day = pendingBetDayRun(startingAt: index, calendar: .current, match: lazyPagingItems.peek(at:)) {
            daySection(day.filter(viewModel.isVisible))
        }
    }

    @ViewBuilder
    private func daySection(_ matches: [PoolGamblerBetModel]) -> some View {
        if let first = matches.first {
            Section {
                ForEach(matches) { match in
                    PendingBetItemView(holder: viewModel.rowHolder(for: match)) { focusedMatch = match.id }
                        .id(match.id)
                        .onGeometryChange(for: CGFloat.self, of: { proxy in proxy.size.height }) { _ in
                            if focusedMatch == match.id { focusedCardResizes += 1 }
                        }
                        .onMatchOpenTap(for: match, onMatchOpen)
                        .padding(.horizontal, boxSpacing.large)
                        .padding(.bottom, cardSpacing)
                        .transition(.opacity)
                }
            } header: {
                PendingBetDayHeader(date: first.matchDateTime)
            }
        }
    }
}

/// The loaded matches of the kickoff day that starts at `index`, in feed order, or nil when the
/// match at `index` is not the first of its day (or is not loaded). The run ends at the next day or
/// at the first slot that is not loaded.
func pendingBetDayRun(
    startingAt index: Int,
    calendar: Calendar,
    match: (Int) -> PoolGamblerBetModel?
) -> [PoolGamblerBetModel]? {
    guard let first = match(index) else { return nil }
    if index > 0, let previous = match(index - 1),
       calendar.isDate(previous.matchDateTime, inSameDayAs: first.matchDateTime) {
        return nil
    }
    return withoutActuallyEscaping(match) { match in
        let sameDay = sequence(state: index + 1) { next -> PoolGamblerBetModel? in
            guard let candidate = match(next),
                  calendar.isDate(candidate.matchDateTime, inSameDayAs: first.matchDateTime) else { return nil }
            next += 1
            return candidate
        }
        return [first] + Array(sameDay)
    }
}

/// Sits after the last loaded slot and reports access to it when it comes on screen, so the pager
/// loads the next page even when the filter hides every card near the end of the list. Keyed by
/// the slot count, so each new page re-arms it.
private struct PendingBetPageTrigger: View {
    let lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolGamblerBetModel>
    let lastIndex: Int

    var body: some View {
        Color.clear
            .frame(height: 1)
            .accessibilityHidden(true)
            .task(id: lastIndex) { await lazyPagingItems.onRowAccess(index: lastIndex) }
    }
}

private let cardSpacing: CGFloat = 12

/// A kickoff day above that day's cards, named relative to today. It pins while its cards scroll,
/// on the screen's own background so cards pass beneath it. The minute clock that drives the
/// cards' lock note also turns "Tomorrow" into "Today" at midnight.
private struct PendingBetDayHeader: View {
    let date: Date

    @Environment(\.boxSpacing) private var boxSpacing
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale

    var body: some View {
        TimelineView(.everyMinute) { context in
            label(PendingBetDayLabel(calendar: calendar, locale: locale).text(for: date, now: context.date))
        }
    }

    private func label(_ text: String) -> some View {
        Text(verbatim: text)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(PendingBetStyle.secondaryText)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, boxSpacing.large + boxSpacing.small)
            .padding(.top, boxSpacing.medium)
            .padding(.bottom, boxSpacing.medium)
            .background(.background)
            .accessibilityAddTraits(.isHeader)
    }
}

/// The message of a filter that keeps no loaded match, in the empty-list style.
private struct PendingBetFilterEmptyMessage: View {
    let filter: PendingBetFilter
    let message: LocalizedStringResource

    var body: some View {
        MessageView(
            icon: Image(sharedResource: filter == .noBet ? .done : .pending),
            message: String(localized: message)
        )
        .foregroundStyle(PendingBetStyle.secondaryText)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 32)
        .padding(.vertical, 64)
        .accessibilityElement(children: .combine)
    }
}

/// Opens the match screen for a loaded entry, upcoming matches included: while betting is open
/// the screen shows the hidden-bets state instead of other participants' predictions.
func invokeMatchOpen(_ handler: MatchOpenHandler?, _ poolGamblerBet: PoolGamblerBetModel) {
    handler?(
        poolGamblerBet.poolId,
        poolGamblerBet.gamblerId,
        poolGamblerBet.matchId
    )
}

private extension View {
    /// Opens the match on tap, but only for locked cards. Open cards hold the score boxes and
    /// steppers, so a card-wide tap recognizer there would compete with them for the tap.
    @ViewBuilder
    func onMatchOpenTap(for poolGamblerBet: PoolGamblerBetModel, _ handler: MatchOpenHandler?) -> some View {
        if poolGamblerBet.isPending {
            self
        } else {
            self
                .contentShape(Rectangle())
                .onTapGesture { invokeMatchOpen(handler, poolGamblerBet) }
        }
    }
}

private struct PendingBetPlaceholderList: View {
    var body: some View {
        ForEach(1...8, id: \.self) { _ in
            PendingBetPlaceholderRow()
        }
    }
}

/// Initial and append loading row: the production card from a placeholder model.
struct PendingBetPlaceholderRow: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        PendingBetItem(
            match: poolGamblerBetPlaceholderModel(),
            draft: placeholderDraft,
            rowState: .locked,
            actions: nil,
            isPlaceholder: true
        )
        .padding(.horizontal, boxSpacing.large)
        .padding(.bottom, cardSpacing)
    }
}

#Preview("List") {
    let repository = PoolGamblerBetFakeRepository()
    PendingBetList(
        viewModel: PendingBetListViewModel(
            getPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase(poolGamblerBetRepository: repository),
            betUseCase: BetUseCase(poolGamblerBetRepository: repository),
            gamblerId: "gambler-id",
            poolId: "pool-id"
        )
    )
}

#Preview("Placeholder") {
    ScrollView {
        LazyVStack(spacing: 0) {
            PendingBetPlaceholderList()
        }
    }
}

#Preview("Empty No bet filter") {
    PendingBetFilterEmptyMessage(filter: .noBet, message: .betsFilterEmptyMessage)
}

#Preview("Empty Placed filter") {
    PendingBetFilterEmptyMessage(filter: .placed, message: .betsFilterPlacedEmptyMessage)
}
