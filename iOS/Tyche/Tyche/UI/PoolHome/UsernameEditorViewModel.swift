import Foundation
import Session
import UI
import ViewingState

@MainActor
class UsernameEditorViewModel: ObservableObject {
    private let updateUsernameUseCase: UpdateUsernameUseCase

    @Published var saveState: LoadState<String> = .idle
    private var lastAttempt: String?

    init(updateUsernameUseCase: UpdateUsernameUseCase) {
        self.updateUsernameUseCase = updateUsernameUseCase
    }

    func save(_ newUsername: String) {
        guard !saveState.isLoading() else { return }
        let trimmed = newUsername.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        lastAttempt = trimmed
        Task { await performSave(trimmed) }
    }

    func retry() {
        guard let lastAttempt, !saveState.isLoading() else { return }
        Task { await performSave(lastAttempt) }
    }

    func resetError() {
        if case .failure = saveState { saveState = .idle }
    }

    func reset() {
        saveState = .idle
        lastAttempt = nil
    }

    private func performSave(_ username: String) async {
        saveState = .loading
        let result = await updateUsernameUseCase.execute(username: username)
        switch result {
        case .success(let saved):
            saveState = .loaded(saved)
        case .failure(let error):
            saveState = .failure(error)
        }
    }
}
