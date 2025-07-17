import SwiftUI

public struct SharedImageResource {
    fileprivate let resource: ImageResource
    
    fileprivate init(_ resource: ImageResource) {
        self.resource = resource
    }
}

public extension SharedImageResource  {
    static let sentimentVerySatisfied = SharedImageResource(.sentimentVerySatisfied)
    static let pending = SharedImageResource(.pending)
    static let done = SharedImageResource(.done)
    static let error = SharedImageResource(.error)
    static let lock = SharedImageResource(.lock)
    static let menu = SharedImageResource(.menu)
    static let arrowForwardIos = SharedImageResource(.arrowForwardIos)
    static let filledAddCircle = SharedImageResource(.filledAddCircle)
}

public extension Image {
    init(sharedResource resource: SharedImageResource) {
        self.init(resource.resource)
    }
}
