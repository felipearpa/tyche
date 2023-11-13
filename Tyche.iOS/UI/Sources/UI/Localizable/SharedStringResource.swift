public struct SharedStringResource {
    let resource: StringResource
    
    init(_ resource: StringResource) {
        self.resource = resource
    }
}

public extension SharedStringResource  {
    static let searchingLabel = SharedStringResource(.searchingLabel)
    static let retryAction = SharedStringResource(.retryAction)
    static let cancelAction = SharedStringResource(.cancelAction)
    static let saveAction = SharedStringResource(.saveAction)
    static let editAction = SharedStringResource(.editAction)
}

public extension String {
    init(sharedResource resource: SharedStringResource) {
        self.init(resource.resource)
    }
}
