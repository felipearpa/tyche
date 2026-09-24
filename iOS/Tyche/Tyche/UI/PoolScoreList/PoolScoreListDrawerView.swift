import SwiftUI
import UI

struct PoolScoreListDrawerView : View {
    @StateObject var viewModel: PoolScoreListDrawerViewModel
    let onSignOut: () -> Void
    let onProfile: () -> Void

    init(
        viewModel: @autoclosure @escaping () -> PoolScoreListDrawerViewModel,
        onSignOut: @escaping () -> Void,
        onProfile: @escaping () -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.onSignOut = onSignOut
        self.onProfile = onProfile
    }

    var body: some View {
        PoolScoreListDrawerStatefulView(
            uiState: viewModel.uiState,
            onProfile: onProfile,
            onSignOut: onSignOut
        )
    }
}

private struct PoolScoreListDrawerStatefulView : View {
    let uiState: PoolScoreListDrawerUiState
    let onProfile: () -> Void
    let onSignOut: () -> Void

    var body: some View {
        DrawerMenu(
            accountId: uiState.accountId,
            username: uiState.username,
            email: uiState.email,
            onProfile: onProfile,
            onSignOut: onSignOut
        )
    }
}

private let previewUiState = PoolScoreListDrawerUiState(
    accountId: "account-1",
    email: "felipearpa@email.com",
    username: "felipearpa"
)

#Preview("Light") {
    PoolScoreListDrawerStatefulView(uiState: previewUiState, onProfile: {}, onSignOut: {})
        .preferredColorScheme(.light)
}

#Preview("Dark") {
    PoolScoreListDrawerStatefulView(uiState: previewUiState, onProfile: {}, onSignOut: {})
        .preferredColorScheme(.dark)
}

#Preview("Largest text in drawer") {
    Color.clear
        .drawer(isShowing: .constant(true)) {
            PoolScoreListDrawerStatefulView(uiState: previewUiState, onProfile: {}, onSignOut: {})
        }
        .dynamicTypeSize(.accessibility5)
}

#Preview("Right-to-left in drawer") {
    Color.clear
        .drawer(isShowing: .constant(true)) {
            PoolScoreListDrawerStatefulView(uiState: previewUiState, onProfile: {}, onSignOut: {})
        }
        .environment(\.layoutDirection, .rightToLeft)
}
