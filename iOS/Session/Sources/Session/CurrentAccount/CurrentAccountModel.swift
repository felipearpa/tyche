import Combine
import Foundation

/// Observable façade over `CurrentAccountCoordinator` for SwiftUI consumers. Replays the
/// current account and stays subscribed for the life of the application; `account` is
/// always published on the main actor.
public final class CurrentAccountModel: ObservableObject {
    @Published public private(set) var account: AccountBundle?

    private var subscription: Task<Void, Never>?

    public init(coordinator: CurrentAccountCoordinator) {
        subscription = Task { @MainActor [weak self] in
            for await account in await coordinator.accountUpdates() {
                self?.account = account
            }
        }
    }

    deinit {
        subscription?.cancel()
    }
}
