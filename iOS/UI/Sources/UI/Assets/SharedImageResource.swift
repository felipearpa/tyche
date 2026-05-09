import SwiftUI

public struct SharedImageResource {
    let resource: ImageResource

    init(_ resource: ImageResource) {
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
    static let home = SharedImageResource(.home)
    static let trophy = SharedImageResource(.trophy)
    static let deleteForever = SharedImageResource(.deleteForever)
    static let personAdd = SharedImageResource(.personAdd)
}

public extension Image {
    init(sharedResource resource: SharedImageResource) {
        self.init(resource.resource)
    }
}
