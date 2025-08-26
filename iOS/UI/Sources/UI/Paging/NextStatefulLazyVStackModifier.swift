import SwiftUI

internal extension View {
    func nextStatefulLazyVStack<Key, Item: Identifiable & Hashable>(
        statefulLazyVStackState: Binding<StatefulLazyVStackState>,
        lazyPagingItems: LazyPagingItems<Key, Item>,
    ) -> some View {
        self.modifier(NextStatefulLazyVStackModifier(
            statefulLazyVStackState: statefulLazyVStackState,
            lazyPagingItems: lazyPagingItems,
        ))
    }
}

private struct NextStatefulLazyVStackModifier<Key, Item: Identifiable & Hashable>: ViewModifier {
    @Binding var statefulLazyVStackState: StatefulLazyVStackState
    @ObservedObject var lazyPagingItems: LazyPagingItems<Key, Item>

    func body(content: Content) -> some View {
        let _ = Self._printChangesIfDebug()

        content
            .task(id: lazyPagingItems.loadState.refresh) {
                switch lazyPagingItems.loadState.refresh {
                case .failure(let error, _):
                    statefulLazyVStackState = .error(error)
                case .notLoading:
                    if lazyPagingItems.loadState.append.endOfPaginationReached && lazyPagingItems.isEmpty {
                        statefulLazyVStackState = .empty
                    } else {
                        if lazyPagingItems.isNotEmpty {
                            statefulLazyVStackState = .content
                        } else {
                            statefulLazyVStackState = .loading
                        }
                    }
                case .loading:
                    break
                }
            }
    }
}
