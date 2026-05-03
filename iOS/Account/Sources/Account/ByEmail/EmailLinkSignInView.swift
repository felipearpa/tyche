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
            email: email,
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
    let email: String
    let onStart: (AccountBundle) -> Void
    let onRetry: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    init(
        viewState: LoadableViewState<AccountBundle>,
        email: String,
        onStart: @escaping (AccountBundle) -> Void = { _ in },
        onRetry: @escaping () -> Void,
    ) {
        self.viewState = viewState
        self.email = email
        self.onStart = onStart
        self.onRetry = onRetry
    }

    var body: some View {
        switch viewState {
        case .initial, .loading:
            LoadingContainerView { EmptyView() }
        case .success(let accountBundle):
            SuccessContent(email: email, start: { onStart(accountBundle) })
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
        VStack(spacing: 0) {
            Spacer()

            ErrorView(localizedError: localizedError)

            Spacer()

            Button(action: onRetry) {
                Text((String(sharedResource: .retryAction)))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .padding(.vertical, boxSpacing.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct SuccessContent: View {
    let email: String
    let start: () -> Void

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: boxSpacing.large) {
                Image(.markEmailRead)
                    .resizable()
                    .frame(width: ICON_SIZE, height: ICON_SIZE)

                VStack(spacing: boxSpacing.medium) {
                    Text(String(.accountVerifiedTitle))
                        .multilineTextAlignment(.center)
                        .font(.title)

                    Text(String(.accountVerifiedDescription))
                        .multilineTextAlignment(.center)
                }

                VerifiedEmailPill(email: email)
            }

            Spacer()

            Button(action: start) {
                Text((String(.startAction)))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .padding(.vertical, boxSpacing.medium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct VerifiedEmailPill: View {
    let email: String

    @Environment(\.boxSpacing) private var boxSpacing

    var body: some View {
        Label {
            Text(email)
        } icon: {
            Image(sharedResource: .done)
                .resizable()
                .frame(width: PILL_ICON_SIZE, height: PILL_ICON_SIZE)
        }
        .font(.subheadline)
        .padding(.horizontal, boxSpacing.medium)
        .padding(.vertical, boxSpacing.small)
        .background(Color(sharedResource: .successContainer))
        .foregroundStyle(Color(sharedResource: .onSuccessContainer))
        .clipShape(RoundedRectangle(cornerRadius: boxSpacing.large))
    }
}

private let ICON_SIZE: CGFloat = 64
private let PILL_ICON_SIZE: CGFloat = 16

#Preview("initial, loading") {
    EmailLinkSignInStatefulView(viewState: .initial, email: "", onRetry: {})
}

#Preview("success") {
    EmailLinkSignInStatefulView(
        viewState: .success(AccountBundle(accountId: "", externalAccountId: "", email: "")),
        email: "preview@example.com",
        onRetry: {},
    )
}

#Preview("failure") {
    EmailLinkSignInStatefulView(
        viewState: .failure(UnknownLocalizedError()),
        email: "preview@example.com",
        onRetry: {},
    )
}
