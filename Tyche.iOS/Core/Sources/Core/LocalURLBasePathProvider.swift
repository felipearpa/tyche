import Foundation

public class LocalURLBasePathProvider : URLBasePathProvider {

    private let basePath: String
    
    public convenience init() {
        self.init(basePath: "https://33a7-181-59-233-121.ngrok-free.app/")
    }
    
    init(basePath: String) {
        self.basePath = basePath
    }
    
    public func prependBasePath(_ string: String) -> URL? {
        return URL(string: basePath + string)
    }
}
