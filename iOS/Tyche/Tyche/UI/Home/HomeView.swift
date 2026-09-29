import SwiftUI
import UI
import Account

struct HomeView<SocialSlot: View>: View {
    let onSignInWithEmail: () -> Void
    let onSignInWithEmailAndPassword: () -> Void
    private let socialSignInSlot: SocialSlot

    @Environment(\.boxSpacing) var boxSpacing

    init(
        onSignInWithEmail: @escaping () -> Void,
        onSignInWithEmailAndPassword: @escaping () -> Void,
        @ViewBuilder socialSignInSlot: () -> SocialSlot
    ) {
        self.onSignInWithEmail = onSignInWithEmail
        self.onSignInWithEmailAndPassword = onSignInWithEmailAndPassword
        self.socialSignInSlot = socialSignInSlot()
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(sharedResource: .primaryContainer),
                    Color(sharedResource: .surface),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: boxSpacing.small) {
                headerSection()
                Spacer()
                informationSection()
                Spacer()
                signInSection()
            }
            .padding(boxSpacing.large)
        }
        // The gradient starts at the dark primary container in both appearances, so the status
        // bar uses light content over it.
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private func headerSection() -> some View {
        HStack(spacing: boxSpacing.small) {
            Image(.tycheLogo)
                .resizable()
                .frame(width: titleIconSize, height: titleIconSize)

            Image(.tycheTitle)
                .resizable()
                .scaledToFit()
                .frame(height: titleIconSize / 2)
        }
        // White over the dark top of the gradient in both appearances (at least 5:1 against the
        // gradient behind the logo).
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, alignment: .center)
    }

    private func informationSection() -> some View {
        Text(.playPoolText)
            .font(.largeTitle)
            .multilineTextAlignment(.center)
    }

    private func signInSection() -> some View {
        VStack(spacing: boxSpacing.medium) {
            Text(.continueWithText)
                .font(.headline)

            Spacer().frame(height: boxSpacing.small)

            // The provider-supplied sign-in button stays outside the glass container.
            socialSignInSlot

            LiquidGlassContainer {
                VStack(spacing: boxSpacing.medium) {
                    Button(action: onSignInWithEmail) {
                        Text(.signInWithEmailAction)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.liquidGlassProminent)

                    Button(action: onSignInWithEmailAndPassword) {
                        Text(.signInWithEmailAndPasswordAction)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.liquidGlass)
                }
            }
        }
    }
}

private let titleIconSize: CGFloat = 64

#Preview("Initial - Light") {
    HomeView(
        onSignInWithEmail: {},
        onSignInWithEmailAndPassword: {},
        socialSignInSlot: {
            SocialSignInRow(googleState: .idle, onSignInWithGoogle: {})
        },
    )
}

#Preview("Initial - Dark") {
    HomeView(
        onSignInWithEmail: {},
        onSignInWithEmailAndPassword: {},
        socialSignInSlot: {
            SocialSignInRow(googleState: .idle, onSignInWithGoogle: {})
        },
    )
    .preferredColorScheme(.dark)
}

#Preview("Loading - Dark") {
    HomeView(
        onSignInWithEmail: {},
        onSignInWithEmailAndPassword: {},
        socialSignInSlot: {
            SocialSignInRow(googleState: .loading, onSignInWithGoogle: {})
        },
    )
    .preferredColorScheme(.dark)
}
