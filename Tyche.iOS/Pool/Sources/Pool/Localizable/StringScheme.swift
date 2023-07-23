import SwiftUI

enum StringScheme: String {

    case positionLabel = "PositionLabel"
    case gamblerPoolListTitle = "GamblerPoolListTitle"

    var localizedKey: LocalizedStringKey {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return LocalizedStringKey(localizedString)
    }
    
    var localizedString: String {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return localizedString
    }
}
