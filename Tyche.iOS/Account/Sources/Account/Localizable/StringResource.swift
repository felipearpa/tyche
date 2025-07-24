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
    static let emailLabel = StringResource(name: "email_label", bundle: Bundle.module)
    static let passwordLabel = StringResource(name: "password_label", bundle: Bundle.module)
    static let signInTitle = StringResource(name: "sign_in_title", bundle: Bundle.module)
    static let signInAction = StringResource(name: "sign_in_action", bundle: Bundle.module)
    static let emailValidationFailureMessage = StringResource(name: "email_validation_failure_message", bundle: Bundle.module)
    static let passwordValidationFailureMessage = StringResource(name: "password_validation_failure_message", bundle: Bundle.module)
    static let successLoginMessage = StringResource(name: "success_login_message", bundle: Bundle.module)
    static let continueAction = StringResource(name: "continue_action", bundle: Bundle.module)
    static let verificationEmailSentTitle = StringResource(name: "verification_email_sent_title", bundle: Bundle.module)
    static let verificationEmailSentDescription = StringResource(name: "verification_email_sent_description", bundle: Bundle.module)
    static let invalidEmailLinkSignInFailureDescription = StringResource(name: "invalid_email_link_sign_in_failure_description", bundle: Bundle.module)
    static let invalidEmailLinkSignInFailureReason = StringResource(name: "invalid_email_link_sign_in_failure_reason", bundle: Bundle.module)
    static let invalidEmailLinkSignInFailureRecoverySuggestion = StringResource(name: "invalid_email_link_sign_in_failure_recovery_suggestion", bundle: Bundle.module)
    static let accountVerifiedTitle = StringResource(name: "account_verified_title", bundle: Bundle.module)
    static let accountVerifiedDescription = StringResource(name: "account_verified_description", bundle: Bundle.module)
    static let startAction = StringResource(name: "start_action", bundle: Bundle.module)
    static let noRecoveryPasswordWarning = StringResource(name: "no_recovery_password_warning", bundle: Bundle.module)
    static let invalidCredentialSignInFailureDescription = StringResource(name: "invalid_credential_sign_in_failure_description", bundle: Bundle.module)
    static let invalidCredentialSignInFailureReason = StringResource(name: "invalid_credential_sign_in_failure_reason", bundle: Bundle.module)
    static let invalidCredentialSignInFailureRecoverySuggestion = StringResource(name: "invalid_credential_sign_in_failure_recovery_suggestion", bundle: Bundle.module)
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name, bundle: resource.bundle)
    }
}
