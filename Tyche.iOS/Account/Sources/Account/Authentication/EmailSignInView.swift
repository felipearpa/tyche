import SwiftUI
import Core
import UI
import Session

private let ICON_SIZE: CGFloat = 64

public struct EmailSignInView: View {
    @StateObject private var viewModel: EmailSignInViewModel
    
    @State private var loginCredential = LoginCredentialModel(username: "", password: "")
    @State private var isValid = false
    
    public init(viewModel: @autoclosure @escaping () -> EmailSignInViewModel) {
        self._viewModel = .init(wrappedValue: viewModel())
    }
    
    public var body: some View {
        EmailSignInStateView(
            viewState: viewModel.state,
            signInWithEmail: viewModel.sendSignInLinkToEmail,
            reset: viewModel.reset
        )
        .navigationTitle(String(.signInTitle))
        .navigationBarBackButtonHidden(viewModel.state.isLoading())
    }
}

private struct EmailSignInStateView: View {
    let viewState: LodableViewState<Void>
    let signInWithEmail: (String) -> Void
    let reset: () -> Void
    
    init(
        viewState: LodableViewState<Void>,
        signInWithEmail: @escaping (String) -> Void = { _ in },
        reset: @escaping () -> Void = {}
    ) {
        self.viewState = viewState
        self.signInWithEmail = signInWithEmail
        self.reset = reset
    }
    
    @State private var email: String = ""
    
    var body: some View {
        let signIn: (() -> Void)? = if Email.isValid(value: email) { { signInWithEmail(email) } } else { nil }
        
        ZStack {
            switch viewState {
            case .initial:
                EmailSignInContent(email: $email, signIn: signIn)
            case .loading:
                ProgressContainerView {
                    EmailSignInContent(email: .constant(email), signIn: {})
                }
            case .success():
                SuccessContent()
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
    @Environment(\.boxSpacing) var boxSpacing
    
    var body: some View {
        VStack(spacing: boxSpacing.large) {
            Image(.outgoingMail)
                .resizable()
                .frame(width: ICON_SIZE, height: ICON_SIZE)
            
            VStack(spacing: boxSpacing.medium) {
                Text(String(.verificationEmailSentTitle))
                    .font(.title)
                
                Text(String(.verificationEmailSentDescription))
                    .multilineTextAlignment(.center)
            }
        }
        .padding(boxSpacing.medium)
    }
}

#Preview("Initial") {
    EmailSignInStateView(viewState: .initial)
}

#Preview("Loading") {
    EmailSignInStateView(viewState: .loading)
}

#Preview("Success") {
    EmailSignInStateView(viewState: .success(()))
}

#Preview("Failure") {
    EmailSignInStateView(viewState: .failure(UnknownLocalizedError()))
}
