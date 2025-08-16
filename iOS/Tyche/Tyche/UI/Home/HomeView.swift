import SwiftUI
import UI
import Account

struct HomeView: View {
    let onSignInWithEmail: () -> Void
    let onSignInWithEmailAndPassword: () -> Void

    @Environment(\.boxSpacing) var boxSpacing

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
    }

    private func headerSection() -> some View {
        HStack(spacing: boxSpacing.small) {
            Image(.tycheLogo)
                .resizable()
                .frame(width: titleIconSize, height: titleIconSize)

            Image(.tycheTitle)
                .resizable()
                .scaledToFit()
                .frame(height: titleIconSize)
        }
        .frame(maxWidth: .infinity, alignment: .center)
    }

    private func informationSection() -> some View {
        Text(String(.playPoolText))
            .font(.largeTitle)
            .multilineTextAlignment(.center)
    }

    private func signInSection() -> some View {
        VStack(spacing: boxSpacing.small) {
            Text(String(.continueWithText))
                .font(.headline)

            Spacer().frame(height: boxSpacing.small)

            Button(action: onSignInWithEmail) {
                Text(String(.signInWithEmailAction))
                    .frame(maxWidth:.infinity)
            }
            .buttonStyle(.borderedProminent)

            Button(action: onSignInWithEmailAndPassword) {
                Text(String(.signInWithEmailAndPasswordAction))
                    .frame(maxWidth:.infinity)
            }
            .buttonStyle(.bordered)
        }
    }
}

private let titleIconSize: CGFloat = 64

#Preview("Light mode") {
    HomeView(onSignInWithEmail: {}, onSignInWithEmailAndPassword: {})
}

#Preview("Dark mode") {
    HomeView(onSignInWithEmail: {}, onSignInWithEmailAndPassword: {})
        .preferredColorScheme(.dark)
}
