import SwiftUI
import Session
import UI

private let ICON_SIZE: CGFloat = 64

public struct EmailLinkSignInView: View {
    @StateObject var viewModel: EmailLinkSignInViewModel
    private let email: String
    private let emailLink: String
    private let onStartRequested: (AccountBundle) -> Void
    
    public init(
        viewModel: @autoclosure @escaping () -> EmailLinkSignInViewModel,
        email: String,
        emailLink: String,
        onStartRequested: @escaping (AccountBundle) -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.email = email
        self.emailLink = emailLink
        self.onStartRequested = onStartRequested
    }
    
    public var body: some View {
        EmailLinkSignInStateView(
            viewState: viewModel.state,
            onStartRequested: onStartRequested
        )
        .onAppearOnce {
            viewModel.signInWithEmailLink(email: email, emailLink: emailLink)
        }
    }
}

private struct EmailLinkSignInStateView: View {
    let viewState: LodableViewState<AccountBundle>
    let onStartRequested: (AccountBundle) -> Void
    
    @Environment(\.boxSpacing) private var boxSpacing
    
    init(
        viewState: LodableViewState<AccountBundle>,
        onStartRequested: @escaping (AccountBundle) -> Void = { _ in }
    ) {
        self.viewState = viewState
        self.onStartRequested = onStartRequested
    }
    
    var body: some View {
        switch viewState {
        case .initial, .loading:
            ProgressContainerView { EmptyView() }
        case .success(let accountBundle):
            SuccessContent(start: { onStartRequested(accountBundle) })
                .padding(boxSpacing.medium)
        case .failure(let error):
            FailureContent(localizedError: error.localizedErrorOrNil()!)
                .padding(boxSpacing.medium)
        }
    }
}

private struct FailureContent: View {
    let localizedError: LocalizedError
    
    var body: some View {
        ErrorView(localizedError: localizedError)
    }
}

private struct SuccessContent: View {
    let start: () -> Void
    
    @Environment(\.boxSpacing) private var boxSpacing
    
    var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(.markEmailRead)
                .resizable()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
            
            VStack(spacing: boxSpacing.medium) {
                Text(String(.accountVerifiedTitle))
                    .font(.headline)
                
                Text(String(.accountVerifiedDescription))
                    .multilineTextAlignment(.center)
            }
            
            Button(action: start) {
                Text((String(.continueAction)))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

#Preview("initial, loading") {
    EmailLinkSignInStateView(viewState: .initial)
}

#Preview("success") {
    EmailLinkSignInStateView(viewState: .success(AccountBundle(accountId: "", externalAccountId: "")))
}

#Preview("failure") {
    EmailLinkSignInStateView(viewState: .failure(UnknownLocalizedError()))
}
