import SwiftUI
import Swinject
import Core
import Session
import Account

struct MainView: View {
    @State var universalLink: URL? = nil

    var body: some View {
        if let emailSignInUniversalLink = universalLink {
            EmailLinkSignInRouter(
                emailSignInURLProcessor: EmailSignInURLProcessor(link: emailSignInUniversalLink),
            )
        } else {
            HomeRouter()
                .onOpenURL { url in
                    universalLink = url
                }
        }
    }
}
