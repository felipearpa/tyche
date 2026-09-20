import Foundation
import Testing
import ViewingState
@testable import Tyche

@MainActor
@Suite("UsernameEditorViewModel")
struct UsernameEditorViewModelTests {
    @Test("given a changed username when saved then the state moves through saving to saved")
    func savingThenSaved() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("neptune")
        await waitUntil { save.isInFlight }

        #expect(viewModel.saveState == .saving("neptune"))

        save.complete(.success("neptune"))
        await waitUntil { viewModel.saveState.isSaved() }

        #expect(viewModel.saveState == .saved("neptune"))
        #expect(save.startedUsernames == ["neptune"])
    }

    @Test("given a save when it fails then the state is failure carrying the attempted value")
    func failurePreservesAttemptedValue() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("neptune")
        await waitUntil { save.isInFlight }
        save.complete(.failure(SampleError.boom))
        await waitUntil { viewModel.saveState.isFailure() }

        guard case let .failure(value, _) = viewModel.saveState else {
            Issue.record("expected failure state")
            return
        }
        #expect(value == "neptune")
    }

    @Test("given a failed save when retried then the same pending value is resubmitted")
    func retryResubmitsFailedValue() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("neptune")
        await waitUntil { save.isInFlight }
        save.complete(.failure(SampleError.boom))
        await waitUntil { viewModel.saveState.isFailure() }

        viewModel.retry()
        await waitUntil { save.isInFlight }

        #expect(viewModel.saveState == .saving("neptune"))

        save.complete(.success("neptune"))
        await waitUntil { viewModel.saveState.isSaved() }

        #expect(save.startedUsernames == ["neptune", "neptune"])
    }

    @Test("given a failure when the error is reset then the state returns to idle")
    func resetErrorReturnsToIdle() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("neptune")
        await waitUntil { save.isInFlight }
        save.complete(.failure(SampleError.boom))
        await waitUntil { viewModel.saveState.isFailure() }

        viewModel.resetError()

        #expect(viewModel.saveState == .idle)
    }

    @Test("given a blank draft when saved then no request is made")
    func blankDraftDoesNotSave() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("   ")
        await waitUntil(timeoutTicks: 20) { false } // give any spawned task a chance to run

        #expect(viewModel.saveState == .idle)
        #expect(save.startedUsernames.isEmpty)
    }

    @Test("given an in-flight save when save is called again then it is ignored")
    func ignoresSaveWhileInFlight() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("neptune")
        await waitUntil { save.isInFlight }

        viewModel.save("other")
        await waitUntil(timeoutTicks: 20) { false }

        #expect(save.startedUsernames == ["neptune"])

        save.complete(.success("neptune"))
        await waitUntil { viewModel.saveState.isSaved() }
    }

    @Test("given a completed save when reset then the state returns to idle")
    func resetClearsState() async {
        let save = ControllableUsernameSave()
        let viewModel = UsernameEditorViewModel(onSave: { await save.operation($0) })

        viewModel.save("neptune")
        await waitUntil { save.isInFlight }
        save.complete(.success("neptune"))
        await waitUntil { viewModel.saveState.isSaved() }

        viewModel.reset()

        #expect(viewModel.saveState == .idle)
    }
}

private enum SampleError: Error {
    case boom
}

/// A controllable stand-in for the username save operation that lets a test observe the
/// in-flight `.saving` state before deciding the outcome.
@MainActor
private final class ControllableUsernameSave {
    private(set) var startedUsernames: [String] = []
    private var continuation: CheckedContinuation<Result<String, Error>, Never>?

    var isInFlight: Bool { continuation != nil }

    func operation(_ username: String) async -> Result<String, Error> {
        startedUsernames.append(username)
        return await withCheckedContinuation { continuation in
            self.continuation = continuation
        }
    }

    func complete(_ result: Result<String, Error>) {
        let pending = continuation
        continuation = nil
        pending?.resume(returning: result)
    }
}

@MainActor
private func waitUntil(timeoutTicks: Int = 1000, _ predicate: () -> Bool) async {
    for _ in 0..<timeoutTicks {
        if predicate() { return }
        await Task.yield()
    }
}
