import Foundation

public protocol URLBasePathProvider {
    func prependBasePath(string: String) -> URL?
}
