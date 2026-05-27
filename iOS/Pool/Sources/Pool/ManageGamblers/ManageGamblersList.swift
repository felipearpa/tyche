import SwiftUI
import Core
import UI
import LazyPaging
import ViewingState

struct ManageGamblersList: View {
    var lazyPagingItems: LazyPaging.LazyPagingItems<String, PoolMemberModel>
    let isEditing: Bool
    let mutationState: (PoolMemberModel) -> MutationState<PoolMemberModel>
    let onRequestRemove: (PoolMemberModel) -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        RefreshableLazyPagingVStack(
            lazyPagingItems: lazyPagingItems,
            loadingContent: { ManageGamblerPlaceholderList() },
            emptyContent: { EmptyView() },
            errorContent: { error in
                LazyPagingVStackError(localizedError: error.orDefaultLocalized())
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
            if let member = lazyPagingItems.peek(at: index) {
                let state = mutationState(member)
                VStack(spacing: 0) {
                    if !state.isMutated() {
                        row(for: member, state: state)
                            .transition(.opacity)
                    }
                }
                .animation(.default, value: state.isMutated())
            }
        }
    }

    @ViewBuilder
    private func row(for member: PoolMemberModel, state: MutationState<PoolMemberModel>) -> some View {
        let deleting = state.isMutating()
        let isRemovable = !member.isOwner

        VStack(spacing: 0) {
            SwipeToDismissBox(
                isEnabled: !deleting && isRemovable,
                onConfirm: {
                    if !deleting && isRemovable {
                        onRequestRemove(member)
                    }
                },
                background: { _, isPastThreshold in
                    ManageGamblerSwipeBackground(isPastThreshold: isPastThreshold)
                },
                content: {
                    HStack(spacing: boxSpacing.medium) {
                        if isEditing && !deleting && isRemovable {
                            Button(action: { onRequestRemove(member) }) {
                                Image(systemName: "minus.circle.fill")
                                    .font(.title3)
                                    .foregroundStyle(Color(sharedResource: .error))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(Text(.removeGamblerAction))
                        }

                        ManageGamblerItem(state: state)
                    }
                    .padding(boxSpacing.medium)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(member.isOwner ? Color(sharedResource: .primaryContainer) : Color(uiColor: .systemBackground))
                }
            )
            .accessibilityElement(children: .combine)
            .accessibilityAction(named: Text(.removeGamblerAction)) {
                if !deleting && isRemovable {
                    onRequestRemove(member)
                }
            }

            Divider()
        }
    }
}

struct ManageGamblerSwipeBackground: View {
    let isPastThreshold: Bool

    var body: some View {
        ZStack(alignment: .trailing) {
            Color(sharedResource: .error)

            Image(systemName: "trash")
                .font(.title3)
                .foregroundStyle(Color.white)
                .scaleEffect(isPastThreshold ? 1.22 : 1.0)
                .animation(.easeOut(duration: 0.1), value: isPastThreshold)
                .padding(.trailing, 26)
        }
    }
}

struct ManageGamblerPlaceholderList: View {
    var body: some View {
        ForEach(0..<8, id: \.self) { index in
            ManageGamblerPlaceholderRow()
                .opacity(1.0 - Double(index) * 0.04)
        }
    }
}

struct ManageGamblerPlaceholderRow: View {
    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: boxSpacing.medium) {
                Circle()
                    .fill(Color(sharedResource: .surfaceVariant))
                    .frame(width: 40, height: 40)

                VStack(alignment: .leading, spacing: boxSpacing.small) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(sharedResource: .surfaceVariant))
                        .frame(width: 160, height: 14)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(sharedResource: .surfaceVariant))
                        .frame(width: 110, height: 12)
                }

                Spacer(minLength: 0)
            }
            .padding(boxSpacing.medium)
            .shimmer()

            Divider()
        }
    }
}

struct ManageGamblerErrorBanner: View {
    let username: String
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        HStack(spacing: boxSpacing.medium) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Color(sharedResource: .error))

            VStack(alignment: .leading, spacing: 2) {
                Text(.removeGamblerFailureTitle("@\(username)"))
                    .fontWeight(.semibold)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Text(.removeGamblerFailureMessage)
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)
                    .lineLimit(2)
            }

            Spacer(minLength: 0)

            Button(action: onRetry) {
                Text(.retryAction)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.white)
                    .padding(.horizontal, boxSpacing.medium)
                    .padding(.vertical, boxSpacing.small)
                    .background(Color(sharedResource: .error), in: Capsule())
            }
        }
        .padding(boxSpacing.medium)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(sharedResource: .surface))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color(sharedResource: .error).opacity(0.4), lineWidth: 1)
                )
        )
        .shadow(radius: 8, y: 4)
    }
}
