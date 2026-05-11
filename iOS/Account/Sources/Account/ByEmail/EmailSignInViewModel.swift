import Foundation
import Core
import UI
import Session
import ViewingState

public class EmailSignInViewModel: ObservableObject {
    @Published @MainActor private(set) var state: LoadState<String> = .idle

    private let sendSignInLinkToEmailUseCase: SendSignInLinkToEmailUseCase

    public init(sendSignInLinkToEmailUseCase: SendSignInLinkToEmailUseCase) {
        self.sendSignInLinkToEmailUseCase = sendSignInLinkToEmailUseCase
    }

    @MainActor
    func reset() {
        state = .idle
    }

    @MainActor
    func sendSignInLinkToEmail(email: String) {
        Task {
            state = .loading

            let result = await sendSignInLinkToEmailUseCase.execute(email: Email(email)!)
            switch result {
            case .success:
                state = .loaded(email)
            case .failure(let error):
                state = .failure(error.mapOrDefaultLocalized { $0.asSendSignInLinkToEmailLocalizedError() })
            }
        }
    }
}
