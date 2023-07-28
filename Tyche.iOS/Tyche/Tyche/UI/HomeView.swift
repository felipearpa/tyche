import SwiftUI
import UI
import User

struct HomeView: View {
    let onLoginRequested: () -> Void
    
    var body: some View {
        VStack {
            HStack {
                ResourceScheme.tycheLogo.image
                    .resizable()
                    .frame(width: 64, height: 64)
                
                ResourceScheme.tycheTitle.image
                    .resizable()
                    .scaledToFit()
                    .frame(height: 64)
            }
            .frame(maxWidth: .infinity, alignment: .center)
            
            Spacer()
            
            Text(StringScheme.playPoolText.localizedKey)
                .font(.largeTitle)
            
            Spacer()
            
            Button(action: {}) {
                Text(StringScheme.createAccountAction.localizedKey)
            }
            
            Spacer()
            
            HStack {
                Text(StringScheme.accountExistsText.localizedKey)
                
                Button(action: { onLoginRequested() }) {
                    Text(StringScheme.logInAction.localizedKey)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding()
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView(onLoginRequested: {})
    }
}
