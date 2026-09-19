import Session
import SwiftUI

/// Pushes `UsernameEditorScreen` only once the shared account has replayed, so the
/// one-shot field seed can never consume an empty username from a not-yet-populated
/// mirror. A signed-in account's username is never empty (it falls back to the email),
/// which makes presence of the account the correct readiness signal.
struct UsernameEditorDestination: View {
    @ObservedObject var currentAccountModel: CurrentAccountModel
    @ObservedObject var viewModel: UsernameEditorViewModel
    let onSaved: (String) -> Void

    var body: some View {
        if let account = currentAccountModel.account {
            UsernameEditorScreen(
                accountId: account.accountId,
                initialUsername: account.username,
                viewModel: viewModel,
                onSaved: onSaved
            )
        } else {
            Color.clear
        }
    }
}

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
