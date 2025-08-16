import SwiftUI
import Swinject
import Core
import Session
import Account

struct RootRouterView: View {
    @State var universalLink: URL? = nil

    var body: some View {
        if let emailSignInUniversalLink = universalLink {
            EmailLinkSignInRouter(
                emailSignInURLProcessor: EmailSignInURLProcessor(link: emailSignInUniversalLink),
            )
        } else {
            SessionRouter()
                .onOpenURL { url in
                    universalLink = url
                }
        }
    }
}
