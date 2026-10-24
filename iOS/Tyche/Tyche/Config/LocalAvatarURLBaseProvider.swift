import Foundation
import Core

class LocalAvatarURLBaseProvider: AvatarURLBaseProvider {
    func callAsFunction() -> String {
        guard let base = Bundle.main.object(forInfoDictionaryKey: "AVATAR_URL_BASE") as? String, !base.isEmpty else {
            fatalError("AVATAR_URL_BASE is missing or empty in Info.plist")
        }
        return base
    }
}
