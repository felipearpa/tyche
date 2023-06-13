import SwiftUI

enum StringScheme: String {

    case remoteCommunicationFailureTitle = "remote_communication_failure_title"
    case remoteCommunicationFailureMessage = "remote_communication_failure_message"
    case unknownFailureTitle = "unknown_failure_title"
    case unknownFailureMessage = "unknown_failure_message"
    case doneAction = "done_action"

    var localizedKey: LocalizedStringKey {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return LocalizedStringKey(localizedString)
    }
    
    var localizedString: String {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return localizedString
    }
}
