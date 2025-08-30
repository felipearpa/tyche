import Foundation
import Core

class LocalJoinPoolUrlTemplateProvider: JoinPoolUrlTemplateProvider {
    func callAsFunction() -> String {
        return Bundle.main.object(forInfoDictionaryKey: "JOIN_POOL_URL_TEMPLATE") as? String ?? ""
    }
}
