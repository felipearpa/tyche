import SwiftUI
import User

struct HomeView: View {
    var body: some View {
        VStack {
            HStack {
                Image(ResourceScheme.tycheLogo.localized)
                    .resizable()
                    .frame(width: 64, height: 64)
                
                Image(ResourceScheme.tycheTitle.localized)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 64)
            }
            .frame(maxWidth: .infinity, alignment: .center)
            
            Spacer()
            
            Text(StringScheme.playPoolText.localizedKey)
                .font(.largeTitle)
            
            Spacer()
            
            Button(action: {
            }) {
                Text(StringScheme.createAccountAction.localizedKey)
                    .foregroundColor(ColorScheme.primaryColor)
            }
            
            Spacer()
            
            HStack {
                Text(StringScheme.accountExistsText.localizedKey)

                NavigationLink(StringScheme.logInAction.localizedKey, value: LoginRoute())
                    .foregroundColor(ColorScheme.primaryColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding()
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
