import SwiftUI
import UIKit

public protocol DrawerStyle {
    associatedtype Body: View
    typealias Configuration = DrawerStyleConfiguration
    func makeBody(configuration: Configuration) -> Self.Body
}

public struct DrawerStyleConfiguration {
    public struct Content: View {
        let view: AnyView
        public var body: some View { view }
    }

    public let content: Content
}

public struct AnyDrawerStyle: DrawerStyle {
    private let _makeBody: (Configuration) -> AnyView

    public init<S: DrawerStyle>(_ style: S) {
        self._makeBody = { AnyView(style.makeBody(configuration: $0)) }
    }

    public func makeBody(configuration: Configuration) -> AnyView {
        _makeBody(configuration)
    }
}

public struct DefaultDrawerStyle: DrawerStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.content
            .background(Color(uiColor: .systemBackground))
    }
}

public extension DrawerStyle where Self == DefaultDrawerStyle {
    static var `default`: DefaultDrawerStyle { DefaultDrawerStyle() }
}

private struct DrawerStyleKey: EnvironmentKey {
    static let defaultValue = AnyDrawerStyle(DefaultDrawerStyle())
}

public extension EnvironmentValues {
    var drawerStyle: AnyDrawerStyle {
        get { self[DrawerStyleKey.self] }
        set { self[DrawerStyleKey.self] = newValue }
    }
}

public extension View {
    func drawerStyle<S: DrawerStyle>(_ style: S) -> some View {
        environment(\.drawerStyle, AnyDrawerStyle(style))
    }
}
