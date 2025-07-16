import SwiftUI
import UI
import Account

struct HomeView: View {
    let onSignInWithEmail: () -> Void
    let onSignInWithEmailAndPassword: () -> Void

    @Environment(\.boxSpacing) var boxSpacing
    
    var body: some View {
        VStack {
            HStack {
                Image(.tycheLogo)
                    .resizable()
                    .frame(width: 64, height: 64)
                
                Image(.tycheTitle)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 64)
            }
            .frame(maxWidth: .infinity, alignment: .center)
            
            Spacer()
            
            Text(String(.playPoolText))
                .font(.largeTitle)
                .multilineTextAlignment(.center)
            
            Spacer()

            VStack(spacing: boxSpacing.medium) {
                Button(action: onSignInWithEmail) {
                    Text(String(.signInWithEmailAction))
                        .frame(maxWidth:.infinity)
                }
                .buttonStyle(.borderedProminent)

                Button(action: onSignInWithEmailAndPassword) {
                    Text(String(.signInWithEmailAndPasswordAction))
                        .frame(maxWidth:.infinity)
                }
            }
        }
        .padding(boxSpacing.large)
    }
}

#Preview {
    HomeView(onSignInWithEmail: {}, onSignInWithEmailAndPassword: {})
}

#Preview {
    HomeView(onSignInWithEmail: {}, onSignInWithEmailAndPassword: {})
        .preferredColorScheme(.dark)
}
