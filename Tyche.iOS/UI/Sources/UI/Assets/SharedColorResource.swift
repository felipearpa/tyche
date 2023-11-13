import SwiftUI

public struct SharedColorResource {
    let resource: ColorResource
    
    init(_ resource: ColorResource) {
        self.resource = resource
    }
}

public extension SharedColorResource  {
    static let onPrimary = SharedColorResource(.onPrimary)
    
    static let primaryContainer = SharedColorResource(.primaryContainer)
    
    static let onPrimaryContainter = SharedColorResource(.onPrimaryContainter)
    
    static let secondaryContainer = SharedColorResource(.secondaryContainer)
    
    static let onSecondaryContainer = SharedColorResource(.onSecondaryContainer)
    
    static let error = SharedColorResource(.error)
}

public extension Color {
    init(sharedResource resource: SharedColorResource) {
        self.init(resource.resource)
    }
}
