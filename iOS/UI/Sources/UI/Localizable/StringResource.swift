import Foundation

struct StringResource {
    fileprivate let name: String.LocalizationValue

    fileprivate let bundle: Bundle

    init(name: String.LocalizationValue, bundle: Bundle) {
        self.name = name
        self.bundle = bundle
    }
}

extension StringResource {
    static let remoteCommunicationFailureTitle = StringResource(name: "remote_communication_failure_title", bundle: Bundle.module)
    static let remoteCommunicationFailureMessage = StringResource(name: "remote_communication_failure_message", bundle: Bundle.module)
    static let unknownFailureDescription = StringResource(name: "unknown_failure_description", bundle: Bundle.module)
    static let unknownFailureReason = StringResource(name: "unknown_failure_reason", bundle: Bundle.module)
    static let unknownFailureRecoverySuggestion = StringResource(name: "unknown_failure_recovery_suggestion", bundle: Bundle.module)
    static let doneAction = StringResource(name: "done_action", bundle: Bundle.module)
    static let emptyListMessage = StringResource(name: "empty_list_message", bundle: Bundle.module)
    static let retryAction = StringResource(name: "retry_action", bundle: Bundle.module)
    static let searchingLabel = StringResource(name: "searching_label", bundle: Bundle.module)
    static let cancelAction = StringResource(name: "cancel_action", bundle: Bundle.module)
    static let saveAction = StringResource(name: "save_action", bundle: Bundle.module)
    static let editAction = StringResource(name: "edit_action", bundle: Bundle.module)
}

extension String {
    init(_ resource: StringResource) {
        self.init(localized: resource.name, bundle: resource.bundle)
    }
}
