import SwiftUI

enum ResourceScheme : String {
    case sentimentVerySatisfied = "SentimentVerySatisfied"
    
    var image: Image {
        return Image(rawValue, bundle: Bundle.module)
    }
}
