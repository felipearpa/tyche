import SwiftUI

/// Hosts the existing `UsernameEditor` form as a pushed screen behind the Profile screen.
struct UsernameEditorScreen: View {
    let initialUsername: String
    @ObservedObject var viewModel: UsernameEditorViewModel
    let onSaved: (String) -> Void
    let onDismiss: () -> Void

    var body: some View {
        ScrollView {
            UsernameEditor(
                initialUsername: initialUsername,
                viewModel: viewModel,
                onSaved: onSaved,
                onDismiss: onDismiss
            )
        }
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
    }
}
