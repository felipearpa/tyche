import SwiftUI

enum StringScheme: LocalizedStringKey {
    case playPoolText = "play_pool_text"
    case createAccountAction = "create_account_action"
    case accountExistsText = "account_exists_text"
    case logInAction = "log_in_action"
    case scoreTab = "score_tab"

    var localizedKey: LocalizedStringKey {
        return rawValue
    }
}
