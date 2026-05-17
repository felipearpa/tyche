import SwiftUI
import UI

private let USERNAME_MAX_GRAPHEMES = 100

struct AccountEditDialog: View {
    let initialUsername: String
    let isSaving: Bool
    let serverError: String?
    let onSave: (String) -> Void
    let onClearError: () -> Void
    let onDismiss: () -> Void

    @State private var draft: String = ""
    @State private var showRequiredError: Bool = false

    @Environment(\.boxSpacing) private var boxSpacing

    private var trimmed: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isRequiredError: Bool {
        showRequiredError && trimmed.isEmpty
    }

    private var errorMessage: String? {
        if isRequiredError { return String(localized: .usernameRequiredError) }
        return serverError
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { if !isSaving { onDismiss() } }

            VStack(alignment: .leading, spacing: boxSpacing.large) {
                HStack(spacing: boxSpacing.medium) {
                    Text(.editProfileTitle)
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    if isSaving {
                        BallSpinner()
                            .frame(width: ICON_SIZE, height: ICON_SIZE)
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    TextField(String(localized: .usernameLabel), text: $draft)
                        .textFieldStyle(.liquidGlass)
                        .disabled(isSaving)
                        .onChange(of: draft) { newValue in
                            let clamped = clampToGraphemes(newValue, max: USERNAME_MAX_GRAPHEMES)
                            if clamped != newValue { draft = clamped }
                            if showRequiredError && !trimmed.isEmpty { showRequiredError = false }
                            if serverError != nil { onClearError() }
                        }

                    if let message = errorMessage {
                        Text(message)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }

                HStack(spacing: boxSpacing.medium) {
                    Spacer()
                    Button(String(sharedResource: .cancelAction), action: onDismiss)
                        .buttonStyle(.bordered)
                        .disabled(isSaving)

                    Button(String(sharedResource: .saveAction), action: save)
                        .buttonStyle(.borderedProminent)
                        .disabled(isSaving)
                }
            }
            .padding(boxSpacing.large)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(uiColor: .systemBackground))
            )
            .padding(.horizontal, boxSpacing.large)
        }
        .onAppear { draft = initialUsername }
    }

    private func save() {
        guard !isSaving else { return }
        if trimmed.isEmpty {
            showRequiredError = true
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

private let ICON_SIZE: CGFloat = 24

#Preview {
    AccountEditDialog(
        initialUsername: "felipearpa",
        isSaving: false,
        serverError: nil,
        onSave: { _ in },
        onClearError: {},
        onDismiss: {}
    )
}

#Preview("Saving") {
    AccountEditDialog(
        initialUsername: "felipearpa",
        isSaving: true,
        serverError: nil,
        onSave: { _ in },
        onClearError: {},
        onDismiss: {}
    )
}

#Preview("With error") {
    AccountEditDialog(
        initialUsername: "felipearpa",
        isSaving: false,
        serverError: "Couldn't save your changes. Please try again.",
        onSave: { _ in },
        onClearError: {},
        onDismiss: {}
    )
}
