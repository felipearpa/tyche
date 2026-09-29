import SwiftUI
import UIKit

/// Follows the drawer's drag with a UIKit pan recognizer, so it takes part in UIKit's gesture
/// arbitration with the scroll views and controls beneath it.
///
/// A SwiftUI drag with high priority is recognized as soon as it travels its minimum distance,
/// whatever its direction, and a scroll view whose pan had not begun by then fails for the rest
/// of the touch, even when the drawer then declines the drag. This recognizer decides before it
/// begins instead: it begins only for a clearly horizontal drag the drawer takes, and every
/// scroll view beneath waits for that decision, so any other drag scrolls from its first
/// movement. Once it begins, it cancels the touch for the control where the drag started, even
/// when the reveal then declines to follow it, as for a drag toward the leading edge while the
/// drawer is closed.
@available(iOS 18, *)
struct DrawerPanGesture: UIGestureRecognizerRepresentable {
    /// The reveal on screen, which decides whether a drag belongs to the drawer.
    let progress: CGFloat
    let isEnabled: Bool
    let handler: DrawerDragHandler

    func makeCoordinator(converter: CoordinateSpaceConverter) -> Coordinator {
        Coordinator(gesture: self)
    }

    func makeUIGestureRecognizer(context: Context) -> UIPanGestureRecognizer {
        let recognizer = UIPanGestureRecognizer()
        recognizer.maximumNumberOfTouches = 1
        recognizer.delegate = context.coordinator
        recognizer.isEnabled = isEnabled
        return recognizer
    }

    func updateUIGestureRecognizer(_ recognizer: UIPanGestureRecognizer, context: Context) {
        context.coordinator.gesture = self
        // Disabling a recognizer mid-drag cancels it, which returns the reveal to its endpoint.
        recognizer.isEnabled = isEnabled
    }

    func handleUIGestureRecognizerAction(_ recognizer: UIPanGestureRecognizer, context: Context) {
        let value = Self.value(of: recognizer, touchDown: context.coordinator.touchDown)
        switch recognizer.state {
        case .began:
            _ = handler.onChange(value, progress, true)
        case .changed:
            _ = handler.onChange(value, progress, false)
        case .ended:
            handler.onEnd(value)
            handler.onFinish()
        case .cancelled, .failed:
            handler.onFinish()
        default:
            break
        }
    }

    /// The drag in window coordinates, which the reveal and the host's reserved band use. The
    /// translation is measured from where the finger touched down: a pan recognizer's own
    /// translation leaves out part of the distance travelled before it began.
    fileprivate static func value(of recognizer: UIPanGestureRecognizer, touchDown: CGPoint) -> DrawerDragValue {
        let location = recognizer.location(in: nil)
        let velocity = recognizer.velocity(in: nil)
        let translation = CGSize(width: location.x - touchDown.x, height: location.y - touchDown.y)
        return DrawerDragValue(
            translation: translation,
            predictedEndTranslation: CGSize(
                width: translation.width + velocity.x * projectionPerVelocity,
                height: translation.height + velocity.y * projectionPerVelocity
            ),
            startLocation: touchDown
        )
    }

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var gesture: DrawerPanGesture
        /// Where the finger of the current drag touched down, in window coordinates.
        fileprivate var touchDown = CGPoint.zero

        init(gesture: DrawerPanGesture) {
            self.gesture = gesture
        }

        /// Called once the finger has moved far enough to be a drag: the drawer begins only for
        /// a drag it takes, and otherwise fails at once, releasing the views waiting on it.
        func gestureRecognizerShouldBegin(_ recognizer: UIGestureRecognizer) -> Bool {
            guard let pan = recognizer as? UIPanGestureRecognizer else { return false }
            return gesture.handler.takesTouch(DrawerPanGesture.value(of: pan, touchDown: touchDown), gesture.progress)
        }

        func gestureRecognizer(_ recognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
            touchDown = touch.location(in: nil)
            return true
        }

        /// Scroll views beneath wait for the drawer to decline before they scroll, so a drag
        /// the drawer takes never scrolls and one it declines never moves the drawer.
        func gestureRecognizer(
            _ recognizer: UIGestureRecognizer,
            shouldBeRequiredToFailBy other: UIGestureRecognizer
        ) -> Bool {
            guard let scrollView = other.view as? UIScrollView else { return false }
            return other === scrollView.panGestureRecognizer
        }
    }
}

/// Seconds of the release velocity added to a drag's translation to project where it would come
/// to rest, matching a scroll view's normal deceleration.
private let projectionPerVelocity: CGFloat = {
    let rate = UIScrollView.DecelerationRate.normal.rawValue
    return rate / (1000 * (1 - rate))
}()
