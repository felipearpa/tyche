import SwiftUI
import Core
import UI
import LazyPaging
import ViewingState

/// The heading followed by one row per member. The heading scrolls with the rows and starts every
/// list state (first load, loaded, failed), so the count stays visible whatever the rows do.
struct ManageGamblersList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolMemberModel>
    let gamblerCount: ViewingState.LoadState<Int?>
    let rowHolder: (PoolMemberModel) -> GamblerRowHolder?
    let isRemoved: (PoolMemberModel) -> Bool
    let onRequestRemove: (PoolMemberModel) -> Void
    let onRefresh: () async -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        let contentInsets = EdgeInsets(top: boxSpacing.medium, leading: 0, bottom: boxSpacing.medium, trailing: 0)
        LazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            contentInsets: contentInsets,
            loadingContent: {
                header
                ManageGamblerPlaceholderList()
            },
            emptyContent: { header },
            errorContent: { error in
                LazyPagingVStackError(
                    localizedError: error.orDefaultLocalized(),
                    retry: { Task { await lazyPagingItems.refresh() } },
                    header: { header }
                )
            },
            prependLoadingContent: { EmptyView() },
            appendLoadingContent: { ManageGamblerPlaceholderRow() },
            prependErrorContent: { _ in EmptyView() },
            appendErrorContent: { error in
                LazyPagingVStackConcatenateError(
                    localizedError: error.localizedErrorOrDefault(),
                    retry: { Task { await lazyPagingItems.retry() } }
                )
            }
        ) { index in
            VStack(spacing: 0) {
                if index == 0 {
                    header
                }
                if let member = lazyPagingItems.peek(at: index) {
                    ManageGamblerMemberRow(
                        member: member,
                        holder: rowHolder(member),
                        isRemoved: isRemoved(member),
                        onRequestRemove: onRequestRemove
                    )
                }
                if index == lazyPagingItems.itemCount - 1 && holdsOnlyTheOwner {
                    NoOtherGamblersView()
                }
            }
        }
        .refreshable { await onRefresh() }
        .fullListStateViewport(contentInsets: contentInsets)
    }

    private var header: some View {
        ManageGamblersHeader(count: gamblerCount)
    }

    private var holdsOnlyTheOwner: Bool {
        let loadState = lazyPagingItems.loadState
        return membersHoldOnlyTheOwner(
            refresh: loadState.refresh,
            append: loadState.append,
            members: lazyPagingItems.loadedItems,
            isRemoved: isRemoved
        )
    }
}

/// Whether every member has loaded and, apart from gamblers removed here, only the owner is left:
/// the condition for showing "No other gamblers yet". Never while a page is loading or after a
/// load error.
func membersHoldOnlyTheOwner(
    refresh: LazyPaging.LoadState,
    append: LazyPaging.LoadState,
    members: [PoolMemberModel],
    isRemoved: (PoolMemberModel) -> Bool
) -> Bool {
    guard refresh.isNotLoading, append.endOfPaginationReached else { return false }
    let remaining = members.filter { !isRemoved($0) }
    return !remaining.isEmpty && remaining.allSatisfy(\.isOwner)
}

/// One member with its separator. The owner's row is the current-user row with no swipe and no
/// "Remove" action. A removed gambler's row fades out and stays hidden.
struct ManageGamblerMemberRow: View {
    let member: PoolMemberModel
    let holder: GamblerRowHolder?
    let isRemoved: Bool
    let onRequestRemove: (PoolMemberModel) -> Void

    var body: some View {
        if member.isOwner {
            VStack(spacing: 0) {
                ManageGamblerItem(state: .idle(member), onRequestRemove: nil, onRetry: {}, onDismissFailure: {})
                ManageGamblerRowDivider()
            }
        } else {
            VStack(spacing: 0) {
                if !isRemoved {
                    VStack(spacing: 0) {
                        GamblerRowStateView(member: member, holder: holder, onRequestRemove: onRequestRemove)
                        ManageGamblerRowDivider()
                    }
                    .transition(.opacity)
                }
            }
            .animation(.easeInOut(duration: gamblerRowExitDuration), value: isRemoved)
        }
    }
}

/// The thin separator below each row, inset by the row's gutter.
struct ManageGamblerRowDivider: View {
    var body: some View {
        Divider()
            .padding(.horizontal, rowHorizontalPadding)
    }
}

/// Shown below the owner's row when nobody else is in the pool.
struct NoOtherGamblersView: View {
    @ScaledMetric(relativeTo: .title2) private var badgeSize: CGFloat = 64
    @ScaledMetric(relativeTo: .title2) private var glyphSize: CGFloat = 32

    var body: some View {
        VStack(spacing: emptyStateSpacing) {
            Image(sharedResource: .group)
                .resizable()
                .frame(width: glyphSize, height: glyphSize)
                .foregroundStyle(Color(sharedResource: .onSurfaceVariant))
                .frame(width: badgeSize, height: badgeSize)
                .background(Circle().fill(Color(sharedResource: .surfaceVariant)))
                .accessibilityHidden(true)

            Text(.noOtherGamblersTitle)
                .font(.title3.bold())
                .foregroundStyle(Color(sharedResource: .onSurface))

            Text(.noOtherGamblersMessage)
                .font(.subheadline)
                .foregroundStyle(Color(sharedResource: .onSurface).opacity(emptyMessageOpacity))
        }
        .multilineTextAlignment(.center)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, emptyStateHorizontalPadding)
        .padding(.vertical, emptyStateVerticalPadding)
        .accessibilityElement(children: .combine)
    }
}

struct ManageGamblerPlaceholderList: View {
    var body: some View {
        ForEach(0..<placeholderRowCount, id: \.self) { _ in
            ManageGamblerPlaceholderRow()
        }
    }
}

/// Initial and append loading row: the production `ManageGamblerItem` with a placeholder model and
/// the loaded row's separator. It has no swipe or remove action.
struct ManageGamblerPlaceholderRow: View {
    let member: PoolMemberModel

    init(member: PoolMemberModel = poolMemberPlaceholderModel()) {
        self.member = member
    }

    var body: some View {
        VStack(spacing: 0) {
            ManageGamblerItem(
                state: .idle(member),
                isPlaceholder: true,
                onRequestRemove: nil,
                onRetry: {},
                onDismissFailure: {}
            )
            ManageGamblerRowDivider()
        }
    }
}

private let placeholderRowCount = 8
private let emptyStateSpacing: CGFloat = 12
private let emptyStateHorizontalPadding: CGFloat = 32
private let emptyStateVerticalPadding: CGFloat = 64
/// `onSurface` at 70 %: 5.9:1 on the light canvas and 7.7:1 on the dark one.
private let emptyMessageOpacity = 0.7

#Preview("No other gamblers") {
    VStack(spacing: 0) {
        ManageGamblersHeader(count: .loaded(1))
        ManageGamblerItem(
            state: .idle(PoolMemberModel(gamblerId: "0", gamblerUsername: "felipearpa", gamblerEmail: "felipe@example.com", isOwner: true)),
            onRequestRemove: nil,
            onRetry: {},
            onDismissFailure: {}
        )
        NoOtherGamblersView()
        Spacer()
    }
}

#Preview("Loading") {
    ScrollView {
        VStack(spacing: 0) {
            ManageGamblersHeader(count: .loading)
            ManageGamblerPlaceholderList()
        }
    }
}
