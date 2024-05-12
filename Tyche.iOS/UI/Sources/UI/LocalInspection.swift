import Foundation

public class LocalInspection {
    public var isPreview: Bool {
        if let xcPreview = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] {
            return xcPreview == "1"
        }
        return false
    }
}
