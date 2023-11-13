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
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name)
    }
}
