import Core
import Pool
import SwiftUI
import UI
import ViewingState

private let spinnerSize: CGFloat = 20

struct UsernameEditor: View {
    let accountId: String
    let initialUsername: String
    @ObservedObject var viewModel: UsernameEditorViewModel
    let onSaved: (String) -> Void

    var body: some View {
        UsernameEditorStatefulView(
            accountId: accountId,
            initialUsername: initialUsername,
            saveState: viewModel.saveState,
            onSave: { viewModel.save($0) },
            onRetry: { viewModel.retry() },
            onDraftChanged: { viewModel.resetError() }
        )
        .onAppear {
            viewModel.reset()
        }
        .onChange(of: viewModel.saveState) { newState in
            newState.onSaved { saved in onSaved(saved) }
        }
    }
}

private struct UsernameEditorStatefulView: View {
    let accountId: String
    let initialUsername: String
    let saveState: SaveState<String>
    let onSave: (String) -> Void
    let onRetry: () -> Void
    let onDraftChanged: () -> Void

    @State private var draft: String = ""
    @State private var fieldInitialization = UsernameFieldInitialization()
    @FocusState private var isFieldFocused: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    private var trimmed: String {
        UsernameDraftRules.trimmed(draft)
    }

    private var isSaving: Bool {
        saveState.isSaving()
    }

    private var failureError: Error? {
        saveState.errorOrNil()
    }

    private var isEmpty: Bool {
        UsernameDraftRules.isEmpty(draft)
    }

    private var canSave: Bool {
        UsernameDraftRules.canSave(draft: draft, initial: initialUsername, isSaving: isSaving)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: boxSpacing.large) {
            Text(.editUsernameSubtitle)
                .font(.body)
                .frame(maxWidth: .infinity, alignment: .leading)

            previewSection

            fieldSection

            actionSection
        }
        .padding(.horizontal, boxSpacing.large)
        .padding(.vertical, boxSpacing.medium)
        .frame(maxWidth: .infinity)
        .onAppear {
            // One-shot per presentation: populate the field first, then focus it. Focusing the
            // already-populated field places the collapsed caret after the final character
            // (position 0 when empty). Later appearances and state updates never re-focus or
            // move a caret/selection the gambler now owns.
            if let initialization = fieldInitialization.takeInitialization(
                initialUsername: initialUsername
            ) {
                draft = initialization.draft
                isFieldFocused = true
            }
        }
    }

    private var previewSection: some View {
        VStack(alignment: .leading, spacing: boxSpacing.small) {
            HStack {
                Text(.usernamePreviewLabel)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(.usernamePreviewLive)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(sharedResource: .currentUser))
            }
            .accessibilityElement(children: .combine)

            GamblerScoreItem(
                poolGamblerScore: UsernamePreview.gamblerScore(
                    accountId: accountId,
                    draft: draft,
                    placeholder: String(localized: .usernamePreviewPlaceholder)
                ),
                isCurrentUser: true
            )
        }
    }

    private var fieldSection: some View {
        VStack(alignment: .leading, spacing: boxSpacing.small) {
            HStack {
                Text(.usernameLabel)
                    .font(.body.weight(.semibold))
                Spacer()
                Text("\(UsernameDraftRules.graphemeCount(draft))/\(UsernameDraftRules.maxGraphemes)")
                    .font(.caption)
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }

            TextField(String(localized: .usernameLabel), text: $draft)
                .textFieldStyle(.liquidGlass)
                .focused($isFieldFocused)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .disabled(isSaving)
                .onSubmit { attemptSave() }
                .onChange(of: draft) { newValue in
                    let clamped = UsernameDraftRules.clamp(newValue)
                    if clamped != newValue { draft = clamped }
                    if failureError != nil { onDraftChanged() }
                }

            guidance
        }
    }

    @ViewBuilder
    private var guidance: some View {
        if let failureError {
            Text(failureMessage(for: failureError))
                .font(.caption)
                .foregroundStyle(Color(sharedResource: .error))
                .frame(maxWidth: .infinity, alignment: .leading)
        } else if isEmpty {
            Text(.usernameEmptyError)
                .font(.caption)
                .foregroundStyle(Color(sharedResource: .error))
                .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            Text(.usernameFieldHelper)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder
    private var actionSection: some View {
        if failureError != nil {
            Button(action: onRetry) {
                actionLabel(Text(.usernameRetryAction))
            }
            .buttonStyle(.liquidGlassProminent)
        } else {
            Button(action: attemptSave) {
                actionLabel(Text(.saveUsernameAction))
            }
            .buttonStyle(.liquidGlassProminent)
            .disabled(!canSave)
            .accessibilityLabel(
                isSaving
                    ? String(localized: .usernameSavingAccessibility)
                    : String(localized: .saveUsernameAction)
            )
        }
    }

    @ViewBuilder
    private func actionLabel(_ label: Text) -> some View {
        ZStack {
            label.opacity(isSaving ? 0 : 1)
            if isSaving {
                BallSpinner()
                    .frame(width: spinnerSize, height: spinnerSize)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func attemptSave() {
        guard canSave else { return }
        onSave(trimmed)
    }

    private func failureMessage(for error: Error) -> LocalizedStringResource {
        error is NetworkError ? .usernameSaveNetworkError : .usernameSaveUnknownError
    }
}

#Preview("idle") {
    UsernameEditorStatefulView(
        accountId: "preview-account",
        initialUsername: "felipearpa",
        saveState: .idle,
        onSave: { _ in },
        onRetry: {},
        onDraftChanged: {}
    )
}

#Preview("saving") {
    UsernameEditorStatefulView(
        accountId: "preview-account",
        initialUsername: "felipearpa",
        saveState: .saving("felipe"),
        onSave: { _ in },
        onRetry: {},
        onDraftChanged: {}
    )
}

#Preview("failure") {
    UsernameEditorStatefulView(
        accountId: "preview-account",
        initialUsername: "felipearpa",
        saveState: .failure(value: "felipe", error: UnknownLocalizedError()),
        onSave: { _ in },
        onRetry: {},
        onDraftChanged: {}
    )
}
