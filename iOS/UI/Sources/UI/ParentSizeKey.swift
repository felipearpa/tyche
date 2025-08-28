import SwiftUI

private struct ParentSizeKey: EnvironmentKey {
    static let defaultValue: CGSize = .zero
}

private struct ParentSafeAreaInsetsKey: EnvironmentKey {
    static let defaultValue: EdgeInsets = .init()
}

public extension EnvironmentValues {
    var parentSize: CGSize {
        get { self[ParentSizeKey.self] }
        set { self[ParentSizeKey.self] = newValue }
    }
    var parentSafeAreaInsets: EdgeInsets {
        get { self[ParentSafeAreaInsetsKey.self] }
        set { self[ParentSafeAreaInsetsKey.self] = newValue }
    }
}

public extension View {
    func withParentGeometryProxy(_ geometryProxy: GeometryProxy) -> some View {
        self
            .environment(\.parentSize, geometryProxy.size)
            .environment(\.parentSafeAreaInsets, geometryProxy.safeAreaInsets)
    }
}
