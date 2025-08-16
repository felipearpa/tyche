import Foundation

public protocol URLBasePathProvider {
    func prependBasePath(_ string: String) -> URL?
}
