import SwiftUI

public enum ResourceScheme: String {
    case arrowDownward = "ArrowDownward"
    case arrowUpward = "ArrowUpward"
    case horizontalRule = "HorizontalRule"
    case sentimentDissatisfied = "SentimentDissatisfied"
    case search = "search"
    
    public var image: Image {
        return Image(rawValue, bundle: Bundle.module)
    }
}
