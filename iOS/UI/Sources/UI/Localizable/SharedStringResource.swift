import SwiftUI

public struct SharedStringResource {
    let resource: LocalizedStringResource

    init(_ resource: LocalizedStringResource) {
        self.resource = resource
    }
}

public extension SharedStringResource {
    static let searchingLabel = SharedStringResource(.searchingLabel)
    static let retryAction = SharedStringResource(.retryAction)
    static let cancelAction = SharedStringResource(.cancelAction)
    static let saveAction = SharedStringResource(.saveAction)
    static let editAction = SharedStringResource(.editAction)
    static let doneAction = SharedStringResource(.doneAction)
}

public extension String {
    init(sharedResource resource: SharedStringResource) {
        self.init(localized: resource.resource)
    }
}

public extension Text {
    init(sharedResource resource: SharedStringResource) {
        self.init(resource.resource)
    }
}
