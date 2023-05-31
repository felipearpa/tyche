import SwiftUI
import ui

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
                    .frame(width: .infinity, height: 64)
            }
            
            Spacer()
            
            Text(StringScheme.playPoolText.localized)
                .font(.largeTitle)
            
            Spacer()
            
            Button(action: {
            }) {
                Text(StringScheme.createAccountAction.localized)
                    .foregroundColor(ColorScheme.primaryColor)
            }
            
            Spacer()
            
            HStack {
                Text(StringScheme.accountExistsText.localized)
                
                Button(action: {
                }) {
                    Text(StringScheme.logInAction.localized)
                        .foregroundColor(ColorScheme.primaryColor)
                }
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
