import SwiftUI

#if DEBUG
public extension View {
    static func _printChangesIfDebug() {
        Self._printChanges()
    }
}
#else
public extension View {
    static func _printChangesIfDebug() {}
}
#endif

#if DEBUG
public extension ViewModifier {
    static func _printChangesIfDebug() {
        Self._printChanges()
    }
}
#else
public extension ViewModifier {
    static func _printChangesIfDebug() {}
}
#endif
