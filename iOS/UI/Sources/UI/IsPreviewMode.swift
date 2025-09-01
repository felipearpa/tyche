import Foundation

public func isInPreviewMode() -> Bool {
    return ProcessInfo.processInfo.isInPreviewMode
}

public extension ProcessInfo {
    var isInPreviewMode : Bool {
        return environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
    }
}
