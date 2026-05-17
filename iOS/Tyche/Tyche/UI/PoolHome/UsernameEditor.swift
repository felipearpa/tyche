import SwiftUI
import UI
import ViewingState

private let USERNAME_MAX_GRAPHEMES = 100
private let iconSize: CGFloat = 24

struct UsernameEditor: View {
    let initialUsername: String
    @ObservedObject var viewModel: UsernameEditorViewModel
    let onSaved: (String) -> Void
    let onDismiss: () -> Void

    var body: some View {
        UsernameEditorStatefulView(
            initialUsername: initialUsername,
            saveState: viewModel.saveState,
            onSave: { viewModel.save($0) },
            onRetry: { viewModel.retry() },
            onResetError: { viewModel.resetError() },
            onDismiss: onDismiss
        )
        .onAppear {
            viewModel.reset()
        }
        .onChange(of: viewModel.saveState) { newState in
            if case .loaded(let saved) = newState { onSaved(saved) }
        }
    }
}

private struct UsernameEditorStatefulView: View {
    let initialUsername: String
    let saveState: LoadState<String>
    let onSave: (String) -> Void
    let onRetry: () -> Void
    let onResetError: () -> Void
    let onDismiss: () -> Void

    @State private var draft: String = ""
    @State private var showRequiredError: Bool = false

    @Environment(\.boxSpacing) private var boxSpacing

    private var trimmed: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isSaving: Bool {
        saveState.isLoading()
    }

    private var failureError: LocalizedError? {
        if case .failure(let error) = saveState {
            return (error as? LocalizedError) ?? UnknownLocalizedError()
        }
        return nil
    }

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.large) {
            HStack(spacing: boxSpacing.medium) {
                Text(.editUsernameTitle)
                    .font(.title)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if isSaving {
                    BallSpinner()
                        .frame(width: iconSize, height: iconSize)
                } else if failureError != nil {
                    Image(sharedResource: .error)
                        .resizable()
                        .frame(width: iconSize, height: iconSize)
                }
            }

            Text(.editUsernameSubtitle)
                .frame(maxWidth: .infinity, alignment: .leading)

            formContent

            VStack(spacing: boxSpacing.small) {
                confirmButton
                dismissButton
            }
        }
        .padding(.horizontal, boxSpacing.large)
        .padding(.vertical, boxSpacing.medium)
        .frame(maxWidth: .infinity)
        .onAppear {
            draft = initialUsername
        }
    }

    private var formContent: some View {
        VStack(alignment: .leading, spacing: 4) {
            TextField(String(localized: .usernameLabel), text: $draft)
                .textFieldStyle(.liquidGlass)
                .disabled(isSaving)
                .onChange(of: draft) { newValue in
                    let clamped = clampToGraphemes(newValue, max: USERNAME_MAX_GRAPHEMES)
                    if clamped != newValue { draft = clamped }
                    if showRequiredError && !trimmed.isEmpty { showRequiredError = false }
                }

            if showRequiredError && trimmed.isEmpty {
                Text(.usernameRequiredError)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
    }

    @ViewBuilder
    private var confirmButton: some View {
        if failureError != nil {
            Button(action: onRetry) {
                Text(sharedResource: .retryAction)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.liquidGlassProminent)
        } else {
            Button(action: save) {
                Text(sharedResource: .saveAction)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.liquidGlassProminent)
            .disabled(isSaving)
        }
    }

    @ViewBuilder
    private var dismissButton: some View {
        if failureError != nil {
            Button(action: onResetError) {
                Text(sharedResource: .cancelAction)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.liquidGlass)
        } else {
            Button(action: onDismiss) {
                Text(sharedResource: .cancelAction)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.liquidGlass)
            .disabled(isSaving)
        }
    }

    private func save() {
        if trimmed.isEmpty {
            showRequiredError = true
            return
        }
        if trimmed == initialUsername.trimmingCharacters(in: .whitespacesAndNewlines) {
            onDismiss()
            return
        }
        onSave(trimmed)
    }
}

private func clampToGraphemes(_ value: String, max: Int) -> String {
    let graphemes = Array(value)
    if graphemes.count <= max { return value }
    return String(graphemes.prefix(max))
}

#Preview("idle") {
    UsernameEditorStatefulView(
        initialUsername: "felipearpa",
        saveState: .idle,
        onSave: { _ in },
        onRetry: {},
        onResetError: {},
        onDismiss: {}
    )
}

#Preview("saving") {
    UsernameEditorStatefulView(
        initialUsername: "felipearpa",
        saveState: .loading,
        onSave: { _ in },
        onRetry: {},
        onResetError: {},
        onDismiss: {}
    )
}

#Preview("failure") {
    UsernameEditorStatefulView(
        initialUsername: "felipearpa",
        saveState: .failure(UnknownLocalizedError()),
        onSave: { _ in },
        onRetry: {},
        onResetError: {},
        onDismiss: {}
    )
}
