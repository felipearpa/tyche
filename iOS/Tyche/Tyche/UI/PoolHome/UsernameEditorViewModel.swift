import Foundation
import UI
import ViewingState

@MainActor
class UsernameEditorViewModel: ObservableObject {
    private let onSave: (String) async -> Result<String, Error>

    @Published var saveState: SaveState<String> = .idle

    init(onSave: @escaping (String) async -> Result<String, Error>) {
        self.onSave = onSave
    }

    func save(_ newUsername: String) {
        guard !saveState.isSaving() else { return }
        let trimmed = newUsername.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task { await performSave(trimmed) }
    }

    func retry() {
        guard case let .failure(value, _) = saveState else { return }
        Task { await performSave(value) }
    }

    func resetError() {
        if saveState.isFailure() { saveState = .idle }
    }

    func reset() {
        saveState = .idle
    }

    private func performSave(_ username: String) async {
        saveState = .saving(username)
        let result = await onSave(username)
        switch result {
        case .success(let saved):
            saveState = .saved(saved)
        case .failure(let error):
            saveState = .failure(value: username, error: error)
        }
    }
}
