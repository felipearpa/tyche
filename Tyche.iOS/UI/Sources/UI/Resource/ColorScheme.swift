import SwiftUI

public enum ColorScheme : String {
    case primary = "primary_color"
    case onPrimary = "OnPrimaryColor"
    case primaryContainer = "PrimaryContainerColor"
    case onPrimaryContainer = "on_primary_containter_color"
    case secondaryContainer = "SecondaryContainerColor"
    case onSecondaryContainer = "on_secondary_container"
    case onSecondary = "OnSecondaryColor"
    case error = "ErrorColor"
    case positive = "PositiveColor"
    case negative = "NegativeColor"
    case neutral = "NeutralColor"
    case shimmer = "ShimmerColor"
    
    public var color: Color {
        return Color(rawValue, bundle: Bundle.module)
    }
}
