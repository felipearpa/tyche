import SwiftUI

public enum StringScheme: LocalizedStringKey {
    case playPoolText = "play_pool_text"
    case createAccountAction = "create_account_action"
    case accountExistsText = "account_exists_text"
    case logInAction = "log_in_action"

    var localized: LocalizedStringKey {
        return rawValue
    }
}
