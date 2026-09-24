import SwiftUI

/// The drawer's model state: the reveal progress SwiftUI animates toward, and the drag that
/// drives it directly while a finger is down.
///
/// Progress is normalized — 0 closed, 1 open — so every visual property derives from the same
/// value and a window resize keeps the reveal where it was. While an animation runs, the value
/// on screen lags `progress`; a drag starts from that presented value, so it takes over an
/// in-flight transition instead of jumping to the transition's target.
struct DrawerReveal: Equatable {
    enum Drag: Equatable {
        /// A horizontal drag moving the reveal from `origin` by its logical translation, both
        /// measured in drawer widths.
        case tracking(origin: CGFloat)
        /// A drag the drawer leaves alone until it ends: the drawer declined it when it started,
        /// had no width yet to follow it with, or a request settled the drawer while the finger
        /// was still down.
        case ignored
    }

    private(set) var progress: CGFloat
    private(set) var drag: Drag?
    /// The endpoint the reveal on screen last came to rest at. A cancelled drag returns there,
    /// even when it took over a transition that was heading for the other endpoint.
    private(set) var settledIsOpen: Bool

    init(isOpen: Bool) {
        progress = isOpen ? 1 : 0
        settledIsOpen = isOpen
    }

    /// A reveal held mid-drag at `progress`, as while a finger rests on the open drawer.
    init(heldAt progress: CGFloat) {
        self.progress = progress.clampedToUnit
        drag = .tracking(origin: self.progress)
        settledIsOpen = true
    }

    var isTracking: Bool {
        if case .tracking = drag { return true }
        return false
    }

    /// Whether the model already rests at the requested endpoint with no drag driving it.
    func isResting(isOpen: Bool) -> Bool {
        !isTracking && progress == Self.endpoint(isOpen: isOpen)
    }

    /// Records that the reveal on screen has come to rest at an endpoint.
    mutating func noteSettled(isOpen: Bool) {
        settledIsOpen = isOpen
    }

    /// Sends the reveal toward an endpoint. A drag still in progress stops driving it.
    mutating func settle(isOpen: Bool) {
        progress = Self.endpoint(isOpen: isOpen)
        if drag != nil {
            drag = .ignored
        }
    }

    /// Applies a drag update. `translation` is logical — positive toward the trailing edge —
    /// and `presentedProgress` is the reveal on screen when the update arrives.
    ///
    /// The first update decides whether the drawer claims the drag. It declines a drag that
    /// starts vertically and, while the drawer rests closed, one toward the leading edge or one
    /// that `startsInExcludedRegion`, the area a host reserves for a bar with its own drags.
    /// Returns `false` when this update declines the drag, so the container can hand the touch
    /// back to the views beneath it. Which drags the drawer claims does not depend on its width;
    /// before it has one, it keeps a drag it claims without moving.
    @discardableResult
    mutating func updateDrag(
        translation: CGSize,
        presentedProgress: CGFloat,
        width: CGFloat,
        startsInExcludedRegion: Bool = false
    ) -> Bool {
        switch drag {
        case nil:
            let presented = presentedProgress.clampedToUnit
            let isHorizontal = abs(translation.width) > abs(translation.height)
            // Closed, the drawer only opens, and it leaves the host's reserved bar alone.
            let restsClosed = progress < 1 && presented <= 0
            let isDeclinedWhileClosed = restsClosed && (translation.width < 0 || startsInExcludedRegion)
            guard isHorizontal, !isDeclinedWhileClosed else {
                drag = .ignored
                return false
            }
            guard width > 0 else {
                drag = .ignored
                return true
            }
            // The origin absorbs the distance the finger travelled before the gesture was
            // recognized, so the reveal continues from what is on screen without a jump.
            drag = .tracking(origin: presented - translation.width / width)
            progress = presented
        case .tracking(let origin):
            if width > 0 {
                progress = (origin + translation.width / width).clampedToUnit
            }
        case .ignored:
            break
        }
        return true
    }

    /// Ends a drag and returns the endpoint its release projects to, or `nil` when the drag
    /// was not driving the reveal. `predictedTranslation` is logical and carries the release
    /// velocity, so a quick flick settles in its direction.
    mutating func endDrag(predictedTranslation: CGFloat, width: CGFloat) -> Bool? {
        defer { drag = nil }
        guard case .tracking(let origin) = drag, width > 0 else { return nil }
        return origin + predictedTranslation / width >= 0.5
    }

    /// Forgets a gesture the system cancelled. When the drag was driving the reveal, returns the
    /// endpoint it must return to: the one it last rested at, not the one a request was heading
    /// for when the drag took over.
    mutating func cancelDrag() -> Bool? {
        defer { drag = nil }
        return isTracking ? settledIsOpen : nil
    }

    /// Converts a screen-space translation to the layout direction's logical axis, where a
    /// positive width points toward the trailing edge.
    static func logicalTranslation(_ translation: CGSize, layoutDirection: LayoutDirection) -> CGSize {
        switch layoutDirection {
        case .rightToLeft:
            return CGSize(width: -translation.width, height: translation.height)
        default:
            return translation
        }
    }

    private static func endpoint(isOpen: Bool) -> CGFloat {
        isOpen ? 1 : 0
    }
}

/// Where the drawer is in its reveal, derived from the progress on screen and the model.
enum DrawerPhase: Equatable {
    case closed
    case opening
    case open
    case closing
    case dragging

    init(presentedProgress: CGFloat, reveal: DrawerReveal) {
        if reveal.isTracking {
            self = .dragging
        } else if reveal.progress >= 1 {
            self = presentedProgress >= 1 ? .open : .opening
        } else {
            self = presentedProgress <= 0 ? .closed : .closing
        }
    }

    /// Outside the closed endpoint the drawer owns interaction: the pushed screen is blocked
    /// and hidden from assistive technology, and only the dismissal surface responds on it.
    var isModal: Bool {
        self != .closed
    }
}

/// Drawer geometry for a window: the menu width and the insets its content keeps clear of.
struct DrawerLayout: Equatable {
    static let widthRatio: CGFloat = 0.85
    static let maximumWidth: CGFloat = 340
    /// The pushed screen always keeps at least a touch target's width visible to dismiss.
    static let minimumDismissalWidth: CGFloat = 44

    let width: CGFloat
    let contentInsets: EdgeInsets
    let viewportHeight: CGFloat

    /// `size` and `safeAreaInsets` come from a proxy that respects the safe area; the drawer
    /// surface itself extends behind it.
    init(size: CGSize, safeAreaInsets: EdgeInsets) {
        let windowWidth = size.width + safeAreaInsets.leading + safeAreaInsets.trailing

        width = max(
            0,
            min(
                windowWidth * Self.widthRatio,
                Self.maximumWidth + safeAreaInsets.leading,
                windowWidth - safeAreaInsets.trailing - Self.minimumDismissalWidth
            )
        )
        contentInsets = EdgeInsets(
            top: safeAreaInsets.top,
            leading: safeAreaInsets.leading,
            bottom: safeAreaInsets.bottom,
            trailing: 0
        )
        viewportHeight = max(0, size.height)
    }
}

extension CGFloat {
    var clampedToUnit: CGFloat {
        Swift.min(Swift.max(self, 0), 1)
    }
}
