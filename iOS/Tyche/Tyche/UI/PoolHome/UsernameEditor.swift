import SwiftUI
import UI
import ViewingState

private let USERNAME_MAX_GRAPHEMES = 100

struct UsernameEditor: View {
    let initialUsername: String
    @ObservedObject var viewModel: UsernameEditorViewModel
    let onSaved: (String) -> Void
    let onDismiss: () -> Void

    @State private var draft: String = ""
    @State private var showRequiredError: Bool = false

    @Environment(\.boxSpacing) private var boxSpacing

    private var trimmed: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isSaving: Bool {
        viewModel.saveState.isLoading()
    }

    private var failureError: LocalizedError? {
        if case .failure(let error) = viewModel.saveState {
            return (error as? LocalizedError) ?? UnknownLocalizedError()
        }
        return nil
    }

    var body: some View {
        Group {
            if isSaving {
                LoadingContainerView { editorLayout }
            } else {
                editorLayout
            }
        }
        .onAppear {
            draft = initialUsername
            viewModel.reset()
        }
        .onChange(of: viewModel.saveState) { newState in
            if case .loaded(let saved) = newState { onSaved(saved) }
        }
    }

    private var editorLayout: some View {
        VStack(alignment: .leading, spacing: boxSpacing.large) {
            Text(.editUsernameTitle)
                .font(.title2)

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)

            VStack(spacing: boxSpacing.small) {
                confirmButton
                dismissButton
            }
        }
        .padding(.horizontal, boxSpacing.large)
        .padding(.vertical, boxSpacing.medium)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private var content: some View {
        if let error = failureError {
            ErrorView(localizedError: error)
                .frame(maxWidth: .infinity)
        } else {
            formContent
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
            Button(String(sharedResource: .retryAction)) {
                viewModel.retry()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .frame(maxWidth: .infinity)
        } else {
            Button(String(sharedResource: .saveAction), action: save)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .frame(maxWidth: .infinity)
                .disabled(isSaving)
        }
    }

    @ViewBuilder
    private var dismissButton: some View {
        if failureError != nil {
            Button(String(sharedResource: .cancelAction)) {
                viewModel.resetError()
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
            .frame(maxWidth: .infinity)
        } else {
            Button(String(sharedResource: .cancelAction), action: onDismiss)
                .buttonStyle(.bordered)
                .controlSize(.large)
                .frame(maxWidth: .infinity)
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
        viewModel.save(trimmed)
    }
}

private func clampToGraphemes(_ value: String, max: Int) -> String {
    let graphemes = Array(value)
    if graphemes.count <= max { return value }
    return String(graphemes.prefix(max))
}
