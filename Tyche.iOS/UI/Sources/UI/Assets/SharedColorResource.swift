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
    static let surfaceVariant = SharedColorResource(.surfaceVariant)
    static let onSurfaceVariant = SharedColorResource(.onSurfaceVariant)
}

public extension Color {
    init(sharedResource resource: SharedColorResource) {
        self.init(resource.resource)
    }
}
