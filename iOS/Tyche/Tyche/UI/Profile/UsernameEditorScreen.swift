import SwiftUI

/// Hosts the redesigned `UsernameEditor` as a pushed screen behind the Profile screen.
///
/// The single scrollable column keeps the in-flow "Save username" action reachable when
/// the keyboard, a small viewport, or large accessibility text reduces available space.
/// While a save is in flight the platform back action is disabled so the request cannot
/// be abandoned mid-flight; success pops back to Profile through `onSaved`.
struct UsernameEditorScreen: View {
    let accountId: String
    let initialUsername: String
    @ObservedObject var viewModel: UsernameEditorViewModel
    let onSaved: (String) -> Void

    private var isSaving: Bool {
        viewModel.saveState.isSaving()
    }

    var body: some View {
        ScrollView {
            UsernameEditor(
                accountId: accountId,
                initialUsername: initialUsername,
                viewModel: viewModel,
                onSaved: onSaved
            )
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
        .navigationTitle(String(localized: .usernameLabel))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(isSaving)
        .interactiveDismissDisabled(isSaving)
    }
}
