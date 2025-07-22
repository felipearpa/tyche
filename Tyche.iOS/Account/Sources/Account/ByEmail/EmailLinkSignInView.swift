import SwiftUI
import Session
import UI

public struct EmailLinkSignInView: View {
    @StateObject var viewModel: EmailLinkSignInViewModel
    private let email: String
    private let emailLink: String
    private let onStart: (AccountBundle) -> Void

    public init(
        viewModel: @autoclosure @escaping () -> EmailLinkSignInViewModel,
        email: String,
        emailLink: String,
        onStart: @escaping (AccountBundle) -> Void
    ) {
        self._viewModel = .init(wrappedValue: viewModel())
        self.email = email
        self.emailLink = emailLink
        self.onStart = onStart
    }

    public var body: some View {
        let signInWithEmailLink = { viewModel.signInWithEmailLink(email: email, emailLink: emailLink) }

        EmailLinkSignInStatefulView(
            viewState: viewModel.state,
            onStart: onStart,
            onRetry: signInWithEmailLink,
        )
        .onAppearOnce {
            signInWithEmailLink()
        }
    }
}

private struct EmailLinkSignInStatefulView: View {
    let viewState: LoadableViewState<AccountBundle>
    let onStart: (AccountBundle) -> Void
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        viewState: LoadableViewState<AccountBundle>,
        onStart: @escaping (AccountBundle) -> Void = { _ in },
        onRetry: @escaping () -> Void,
    ) {
        self.viewState = viewState
        self.onStart = onStart
        self.onRetry = onRetry
    }

    var body: some View {
        switch viewState {
        case .initial, .loading:
            LoadingContainerView { EmptyView() }
        case .success(let accountBundle):
            SuccessContent(start: { onStart(accountBundle) })
                .padding(boxSpacing.medium)
        case .failure(let error):
            FailureContent(
                localizedError: error.localizedErrorOrDefault(),
                onRetry: onRetry,
            )
            .padding(boxSpacing.medium)
        }
    }
}

private struct FailureContent: View {
    let localizedError: LocalizedError
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            ErrorView(localizedError: localizedError)

            Button(action: onRetry) {
                Text((String(sharedResource: .retryAction)))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
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
                    .multilineTextAlignment(.center)
                    .font(.title)

                Text(String(.accountVerifiedDescription))
                    .multilineTextAlignment(.leading)
            }

            Button(action: start) {
                Text((String(.continueAction)))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

private let ICON_SIZE: CGFloat = 64

#Preview("initial, loading") {
    EmailLinkSignInStatefulView(viewState: .initial, onRetry: {})
}

#Preview("success") {
    EmailLinkSignInStatefulView(
        viewState: .success(AccountBundle(accountId: "", externalAccountId: "")),
        onRetry: {},
    )
}

#Preview("failure") {
    EmailLinkSignInStatefulView(
        viewState: .failure(UnknownLocalizedError()),
        onRetry: {},
    )
}
