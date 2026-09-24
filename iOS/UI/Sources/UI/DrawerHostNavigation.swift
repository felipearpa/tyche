import SwiftUI

/// Where a drawer host stands: the destinations shown above it and whether its drawer is open.
///
/// A host keeps this as its state, binds `path` to its `NavigationStack` and `isDrawerOpen` to
/// `drawer(isShowing:allowsDragging:content:)`, and acts through the members below rather than
/// by changing either directly.
///
/// Controls respond according to the last update SwiftUI rendered, so two activations handled
/// before the next update, such as two fingers lifting in the same touch event, both run: a
/// drawer row that was just chosen, or a host row that just opened a destination, still
/// responds. Each member checks this state as it is when the action runs, where the first
/// activation is already recorded, so the second does nothing. Nothing stays blocked: once the
/// host is the visible screen again, or the drawer reopens, the next choice goes through.
public struct DrawerHostNavigation {
    /// The destinations shown above the host.
    public var path = NavigationPath()
    /// Whether the drawer is open or opening.
    public var isDrawerOpen = false

    public init() {}

    /// Whether the host is the visible screen, with no destination above it. The host passes it
    /// as the drawer's `allowsDragging`.
    public var isHostVisible: Bool {
        path.isEmpty
    }

    /// Whether the host's own controls may act: it is the visible screen and its drawer is
    /// closed. A host control that leaves in some other way than opening a destination, such as
    /// replacing the host, checks it first.
    public var isHostInteractive: Bool {
        isHostVisible && !isDrawerOpen
    }

    /// Opens `route` from one of the host's own controls, only while the host is interactive.
    public mutating func open<Route: Hashable>(_ route: Route) {
        guard isHostInteractive else { return }
        path.append(route)
    }

    /// Opens or closes the drawer from the host's menu control, only while the host is the
    /// visible screen, so the drawer never opens above a destination.
    public mutating func toggleDrawer() {
        guard isHostVisible else { return }
        isDrawerOpen.toggle()
    }

    /// Closes the drawer for a choice made in it and returns whether the choice should run: only
    /// while the drawer is still open, because an earlier choice closes it.
    public mutating func closeDrawerForChoice() -> Bool {
        guard isDrawerOpen else { return false }
        isDrawerOpen = false
        return true
    }

    /// Closes the drawer and opens `route`, chosen in the drawer, once.
    public mutating func openFromDrawer<Route: Hashable>(_ route: Route) {
        guard closeDrawerForChoice() else { return }
        path.append(route)
    }
}
