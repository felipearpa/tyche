import SwiftUI
import UI
import Account

struct HomeView: View {
    let onSignInRequested: () -> Void
    
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
            
            Button(action: onSignInRequested) {
                Text(String(.signInWithEmailAction))
                    .frame(maxWidth:.infinity)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(boxSpacing.large)
    }
}

#Preview {
    HomeView(onSignInRequested: {})
}

#Preview {
    HomeView(onSignInRequested: {})
        .preferredColorScheme(.dark)
}
