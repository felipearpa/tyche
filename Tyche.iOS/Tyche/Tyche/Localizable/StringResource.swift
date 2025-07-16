import SwiftUI

struct StringResource {
    fileprivate let name: String.LocalizationValue

    init(name: String.LocalizationValue) {
        self.name = name
    }
}

extension StringResource {
    static let playPoolText = StringResource(name: "play_pool_text")
    static let createAccountAction = StringResource(name: "create_account_action")
    static let accountExistsText = StringResource(name: "account_exists_text")
    static let logInAction = StringResource(name: "log_in_action")
    static let scoreTab = StringResource(name: "score_tab")
    static let betTab = StringResource(name: "bet_tab")
    static let signInWithEmailAction = StringResource(name: "sign_in_with_email_action")
    static let signInWithEmailAndPasswordAction = StringResource(name: "sign_in_with_email_and_password_action")
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name)
    }
}
