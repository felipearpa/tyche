import Foundation
import Session

class LocalSignInLinkUrlTemplateProvider: SignInLinkUrlTemplateProvider {
    func callAsFunction() -> String {
        return Bundle.main.object(forInfoDictionaryKey: "SIGN_IN_URL_TEMPLATE") as? String ?? ""
    }
}
