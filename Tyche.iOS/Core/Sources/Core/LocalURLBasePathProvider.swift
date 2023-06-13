import Foundation

public class LocalURLBasePathProvider : URLBasePathProvider {

    public init() {}
    
    public func prependBasePath(string: String) -> URL? {
        return URL(string: "https://d700-181-59-233-121.ngrok-free.app/" + string)
    }
}
