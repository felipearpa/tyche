import SwiftUI

enum StringScheme: String {
    case usernameText = "username_text"
    case passwordText = "password_text"
    case loginTitle = "login_title"
    case loginAction = "login_action"
    case usernameValidationFailureMessage = "username_validation_failure_message"
    case passwordValidationFailureMessage = "password_validation_failure_message"
    case invalidCredentialFailureTitle = "invalid_credential_failure_title"
    case invalidCredentialFailureMessage = "invalid_credential_failure_message"

    var localizedKey: LocalizedStringKey {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return LocalizedStringKey(localizedString)
    }
    
    var localizedString: String {
        return NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
    }
}
