import SwiftUI

public enum ColorScheme : String {
    case primary = "primary_color"
    case primaryContainer = "PrimaryContainerColor"
    case onPrimary = "OnPrimaryColor"
    case secondaryContainer = "SecondaryContainerColor"
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
