import Foundation

public class LocalURLBasePathProvider : URLBasePathProvider {

    private let basePath: String
    
    public convenience init() {
        self.init(basePath: "https://guiding-terminally-cicada.ngrok-free.app/")
    }
    
    init(basePath: String) {
        self.basePath = basePath
    }
    
    public func prependBasePath(_ string: String) -> URL? {
        return URL(string: basePath + string)
    }
}
