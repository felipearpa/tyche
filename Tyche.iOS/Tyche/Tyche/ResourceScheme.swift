import SwiftUI

enum ResourceScheme : String {
    case tycheLogo = "TycheLogo"
    case tycheTitle = "TycheTitle"
    case sportScore = "sport_score"
    
    var name: String {
        return rawValue
    }
    
    var image: Image {
        return Image(rawValue)
    }
}
