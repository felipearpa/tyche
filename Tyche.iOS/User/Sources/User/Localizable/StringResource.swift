import SwiftUI

struct StringResource {
    fileprivate let name: String.LocalizationValue

    fileprivate let bundle: Bundle

    init(name: String.LocalizationValue, bundle: Bundle) {
        self.name = name
        self.bundle = bundle
    }
}

extension StringResource {
    static let usernameText = StringResource(name: "username_text", bundle: Bundle.module)
    
    static let passwordText = StringResource(name: "password_text", bundle: Bundle.module)
    
    static let loginTitle = StringResource(name: "login_title", bundle: Bundle.module)
    
    static let loginAction = StringResource(name: "login_action", bundle: Bundle.module)
    
    static let usernameValidationFailureMessage = StringResource(name: "username_validation_failure_message", bundle: Bundle.module)
    
    static let passwordValidationFailureMessage = StringResource(name: "password_validation_failure_message", bundle: Bundle.module)
    
    static let invalidCredentialFailureTitle = StringResource(name: "invalid_credential_failure_title", bundle: Bundle.module)
    
    static let invalidCredentialFailureMessage = StringResource(name: "invalid_credential_failure_message", bundle: Bundle.module)
    
    static let successLoginMessage = StringResource(name: "success_login_message", bundle: Bundle.module)
    
    static let continueAction = StringResource(name: "continue_action", bundle: Bundle.module)
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name, bundle: resource.bundle)
    }
}
