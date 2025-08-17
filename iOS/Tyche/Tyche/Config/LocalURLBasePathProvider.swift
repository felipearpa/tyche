import Foundation
import Core

public class LocalURLBasePathProvider : URLBasePathProvider {
    private let basePath: String

    public convenience init() {
        let basePath = (Bundle.main.object(forInfoDictionaryKey: "URL_BASE_PATH") as? String) ?? ""
        self.init(basePath: basePath)
    }

    init(basePath: String) {
        if basePath.hasSuffix("/") {
            self.basePath = basePath
        } else {
            self.basePath = basePath + "/"
        }
    }

    public func prependBasePath(_ string: String) -> URL? {
        return URL(string: basePath + string)
    }
}
