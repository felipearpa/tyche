import SwiftUI

public struct EditableInlineText: View {
    private let value: String
    private let onSave: (String) -> Void
    private let isSaving: Bool
    private let maxGraphemes: Int
    private let font: Font
    private let placeholder: String

    @State private var isEditing: Bool = false
    @State private var draft: String = ""
    @FocusState private var isFocused: Bool

    @Environment(\.boxSpacing) private var boxSpacing

    public init(
        value: String,
        onSave: @escaping (String) -> Void,
        isSaving: Bool,
        maxGraphemes: Int = .max,
        font: Font = .body,
        placeholder: String = ""
    ) {
        self.value = value
        self.onSave = onSave
        self.isSaving = isSaving
        self.maxGraphemes = maxGraphemes
        self.font = font
        self.placeholder = placeholder
    }

    public var body: some View {
        HStack(spacing: boxSpacing.small) {
            if isEditing {
                ZStack {
                    TextField(placeholder, text: $draft)
                        .font(font)
                        .focused($isFocused)
                        .disabled(isSaving)
                        .submitLabel(.done)
                        .onSubmit { commit() }
                        .onChange(of: draft) { newValue in
                            let clamped = clampToGraphemes(newValue, max: maxGraphemes)
                            if clamped != newValue { draft = clamped }
                        }

                    if isSaving {
                        ProgressView()
                            .controlSize(.small)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: commit) {
                    Image(systemName: "checkmark")
                }
                .disabled(isSaving)

                Button(action: cancel) {
                    Image(systemName: "xmark")
                }
                .disabled(isSaving)
            } else {
                Text(value.isEmpty ? placeholder : value)
                    .font(font)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Button(action: startEditing) {
                    Image(systemName: "pencil")
                }
            }
        }
        .onChange(of: value) { newValue in
            if isEditing && !isSaving && draft == newValue {
                isEditing = false
            }
        }
        .onChange(of: isSaving) { saving in
            if isEditing && !saving && draft == value {
                isEditing = false
            }
        }
    }

    private func startEditing() {
        draft = value
        isEditing = true
        DispatchQueue.main.async { isFocused = true }
    }

    private func commit() {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        if trimmed != value {
            onSave(trimmed)
        } else {
            isEditing = false
        }
    }

    private func cancel() {
        guard !isSaving else { return }
        draft = value
        isEditing = false
    }
}

private func clampToGraphemes(_ value: String, max: Int) -> String {
    if max == .max { return value }
    let graphemes = Array(value)
    if graphemes.count <= max { return value }
    return String(graphemes.prefix(max))
}
