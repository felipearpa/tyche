import SwiftUI
import UI
import Account

struct HomeView: View {
    let onLoginRequested: () -> Void
    
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
            
            Spacer()
            
            Button(action: {}) {
                Text(String(.createAccountAction))
            }
            
            Spacer()
            
            HStack {
                Text(String(.accountExistsText))
                
                Button(action: { onLoginRequested() }) {
                    Text(String(.logInAction))
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
