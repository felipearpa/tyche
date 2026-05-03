import SwiftUI
import Core
import UI
import Session

public struct EmailSignInView: View {
    @StateObject private var viewModel: EmailSignInViewModel

    public init(viewModel: @autoclosure @escaping () -> EmailSignInViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }

    public var body: some View {
        EmailSignInStatefulView(
            viewState: viewModel.state,
            signInWithEmail: viewModel.sendSignInLinkToEmail,
            reset: viewModel.reset
        )
        .navigationTitle(String(.signInTitle))
        .navigationBarBackButtonHidden(viewModel.state.isLoading())
    }
}

private struct EmailSignInStatefulView: View {
    let viewState: LoadableViewState<String>
    let signInWithEmail: (String) -> Void
    let reset: () -> Void

    init(
        viewState: LoadableViewState<String>,
        signInWithEmail: @escaping (String) -> Void = { _ in },
        reset: @escaping () -> Void = {}
    ) {
        self.viewState = viewState
        self.signInWithEmail = signInWithEmail
        self.reset = reset
    }

    @State private var email: String = ""

    var body: some View {
        let signIn: (() -> Void)? = if Email.isValid(email) { { signInWithEmail(email) } } else { nil }

        ZStack {
            switch viewState {
            case .initial:
                EmailSignInContent(email: $email, signIn: signIn)
            case .loading:
                LoadingContainerView {
                    EmailSignInContent(email: .constant(email), signIn: {})
                }
            case .success(let sentEmail):
                SuccessContent(email: sentEmail)
            case .failure(let error):
                EmailSignInContent(email: .constant(email), signIn: {})
                    .errorAlert(.constant(error.localizedErrorOrNil()!), onDismiss: reset)
            }
        }
        .navigationTitle(String(.signInTitle))
        .navigationBarBackButtonHidden(viewState.isLoading())
    }
}

private struct EmailSignInContent: View {
    @Binding var email: String
    let signIn: (() -> Void)?

    @Environment(\.boxSpacing) var boxSpacing;

    var body: some View {
        VStack(spacing: boxSpacing.medium) {
            EmailTextField(value: $email)

            Button(action: signIn ?? {}) {
                Text((String(.signInAction)))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(signIn == nil)

            Spacer()
        }
        .padding(boxSpacing.medium)
        .textFieldStyle(.roundedBorder)
    }
}

private struct SuccessContent: View {
    let email: String

    @Environment(\.boxSpacing) var boxSpacing

    var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(.outgoingMail)
                .resizable()
                .frame(width: EMAIL_ICON_SIZE, height: EMAIL_ICON_SIZE)

            VStack(spacing: boxSpacing.medium) {
                Text(String(.verificationEmailSentTitle))
                    .font(.title)
                    .multilineTextAlignment(.center)

                Text(String(.verificationEmailSentDescription))
                    .multilineTextAlignment(.leading)

                Label {
                    Text(email)
                } icon: {
                    Image(.mail)
                }
                .font(.subheadline)
                .padding(.horizontal, boxSpacing.medium)
                .padding(.vertical, boxSpacing.small)
                .overlay(
                    RoundedRectangle(cornerRadius: boxSpacing.large)
                        .stroke(Color(.systemGray4))
                )
                .padding(.bottom, boxSpacing.medium)

                Label {
                    Text(String(.verificationEmailSentWarning))
                } icon: {
                    Image(.warning)
                        .resizable()
                        .frame(width: WARNING_ICON_SIZE, height: WARNING_ICON_SIZE)
                        .foregroundStyle(Color(sharedResource: .onWarningContainer))
                }
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .padding(boxSpacing.medium)
                .background(Color(sharedResource: .warningContainer))
                .foregroundStyle(Color(sharedResource: .onWarningContainer))
                .clipShape(RoundedRectangle(cornerRadius: boxSpacing.medium))
            }
        }
        .padding(boxSpacing.medium)
    }
}

private let EMAIL_ICON_SIZE: CGFloat = 64
private let WARNING_ICON_SIZE: CGFloat = 16

#Preview("Initial") {
    EmailSignInStatefulView(viewState: .initial)
}

#Preview("Loading") {
    EmailSignInStatefulView(viewState: .loading)
}

#Preview("Success") {
    EmailSignInStatefulView(viewState: .success("preview@example.com"))
}

#Preview("Failure") {
    EmailSignInStatefulView(viewState: .failure(UnknownLocalizedError()))
}
