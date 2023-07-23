import SwiftUI

public enum StringScheme: String {

    case remoteCommunicationFailureTitle = "remote_communication_failure_title"
    case remoteCommunicationFailureMessage = "remote_communication_failure_message"
    case unknownFailureTitle = "unknown_failure_title"
    case unknownFailureMessage = "unknown_failure_message"
    case doneAction = "done_action"
    case emptyListMessage = "empty_list_message"
    case retryAction = "retry_action"
    case searchingLabel = "searching_label"

    public var localizedKey: LocalizedStringKey {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return LocalizedStringKey(localizedString)
    }
    
    public var localizedString: String {
        let localizedString = NSLocalizedString(self.rawValue, bundle: Bundle.module, comment: "")
        return localizedString
    }
}
