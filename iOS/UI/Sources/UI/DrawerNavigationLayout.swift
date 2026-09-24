import SwiftUI
import UIKit

/// A fixed hosting boundary keeps SwiftUI from extending a translated NavigationStack back to
/// the window's safe-area edge. UIKit still supplies the native bars and destination gestures.
struct DrawerNavigationHost<Content: View>: UIViewControllerRepresentable {
    let content: Content
    let displacement: CGFloat
    let safeAreaInsets: EdgeInsets
    let isModal: Bool
    @Environment(\.self) private var environment

    func makeUIViewController(context: Context) -> DrawerNavigationHostingController {
        let controller = DrawerNavigationHostingController(rootView: hostedContent)
        update(controller)
        return controller
    }

    func updateUIViewController(_ controller: DrawerNavigationHostingController, context: Context) {
        controller.rootView = hostedContent
        update(controller)
    }

    static func dismantleUIViewController(_ controller: DrawerNavigationHostingController, coordinator: ()) {
        controller.stopObservingNavigationLayout()
    }

    private var hostedContent: AnyView {
        AnyView(DrawerHostedContent(content: content, sourceEnvironment: environment))
    }

    private func update(_ controller: DrawerNavigationHostingController) {
        controller.displacement = displacement
        controller.containerInsets = safeAreaInsets
        controller.layoutDirection = environment.layoutDirection
        controller.view.accessibilityElementsHidden = isModal
        controller.updateNavigationLayout()
    }
}

private struct DrawerHostedContent<Content: View>: View {
    let content: Content
    let sourceEnvironment: EnvironmentValues

    var body: some View {
        // A hosting boundary needs its own geometry and accessibility ancestry. Copy only
        // presentation values and the drawer's explicit communication with its content.
        content
            .environment(\.boxSpacing, sourceEnvironment.boxSpacing)
            .environment(\.parentSize, sourceEnvironment.parentSize)
            .environment(\.parentSafeAreaInsets, sourceEnvironment.parentSafeAreaInsets)
            .environment(\.drawerFocus, sourceEnvironment.drawerFocus)
            .environment(\.drawerExcludedBandReporter, sourceEnvironment.drawerExcludedBandReporter)
            .environment(\.layoutDirection, sourceEnvironment.layoutDirection)
            .environment(\.locale, sourceEnvironment.locale)
            .environment(\.colorScheme, sourceEnvironment.colorScheme)
            .environment(\.dynamicTypeSize, sourceEnvironment.dynamicTypeSize)
            .environment(\.isEnabled, sourceEnvironment.isEnabled)
    }
}

final class DrawerNavigationHostingController: UIViewController {
    private let hosting: UIHostingController<AnyView>

    var rootView: AnyView {
        get { hosting.rootView }
        set { hosting.rootView = newValue }
    }

    var displacement: CGFloat = 0
    var containerInsets = EdgeInsets()
    var layoutDirection = LayoutDirection.leftToRight

    private weak var navigation: UINavigationController?
    private var contentMargins = NSDirectionalEdgeInsets.zero
    private var originalMargins = NSDirectionalEdgeInsets.zero
    private var originalAdditionalInsets = UIEdgeInsets.zero
    private var originalRespectsSystemMargins = true
    private var originalInsetsMarginsFromSafeArea = true
    private var isCorrecting = false
    private var isUpdating = false
    private let safeAreaObserver = DrawerSafeAreaObserver()

    init(rootView: AnyView) {
        hosting = UIHostingController(rootView: rootView)
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is unavailable") }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(hosting)
        hosting.view.frame = view.bounds
        hosting.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(hosting.view)
        hosting.didMove(toParent: self)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateNavigationLayout()
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateNavigationLayout()
    }

    func updateNavigationLayout() {
        guard !isUpdating, isViewLoaded else { return }
        isUpdating = true
        defer { isUpdating = false }
        // Search only the controller hierarchy owned by this foreground host, never the window
        // or a presented sheet. No private UIKit/SwiftUI class names are involved.
        guard let current = firstNavigationController(in: self), current.isViewLoaded else { return }
        if navigation !== current {
            restoreNavigationLayout()
            safeAreaObserver.removeFromSuperview()
            navigation = current
            recordBaseline(current)
            safeAreaObserver.frame = current.view.bounds
            safeAreaObserver.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            safeAreaObserver.isUserInteractionEnabled = false
            safeAreaObserver.accessibilityElementsHidden = true
            safeAreaObserver.onChange = { [weak self] in self?.updateNavigationLayout() }
            current.view.insertSubview(safeAreaObserver, at: 0)
        }
        guard displacement > 0 else {
            restoreNavigationLayout()
            recordBaseline(current)
            return
        }

        UIView.performWithoutAnimation {
            if !isCorrecting {
                originalAdditionalInsets = current.additionalSafeAreaInsets
                originalRespectsSystemMargins = current.viewRespectsSystemMinimumLayoutMargins
                originalInsetsMarginsFromSafeArea = current.view.insetsLayoutMarginsFromSafeArea
                current.viewRespectsSystemMinimumLayoutMargins = false
                current.view.insetsLayoutMarginsFromSafeArea = false
                // The getter otherwise includes the safe area and system minimum. Save the raw
                // setting, so restoring it does not add the safe area a second time.
                originalMargins = current.view.directionalLayoutMargins
                isCorrecting = true
            }
            var margins = contentMargins
            margins.top += current.view.safeAreaInsets.top
            margins.bottom += current.view.safeAreaInsets.bottom
            margins.leading += containerInsets.leading
            margins.trailing += containerInsets.trailing
            var additional = originalAdditionalInsets
            // This is the current SwiftUI animation frame, before the host moves. Reading the
            // controller's old frame here would lag one frame when the drawer reverses.
            let compensation: CGFloat
            if #available(iOS 26, *) {
                compensation = min(containerInsets.leading, displacement)
            } else {
                // Earlier UIKit versions update the inherited safe area after translation.
                // Correct what is missing now rather than anticipating the next frame.
                let inherited = layoutDirection == .leftToRight
                    ? current.view.safeAreaInsets.left - current.additionalSafeAreaInsets.left
                    : current.view.safeAreaInsets.right - current.additionalSafeAreaInsets.right
                compensation = min(containerInsets.leading, max(0, containerInsets.leading - inherited))
            }
            if layoutDirection == .leftToRight {
                additional.left += compensation
            } else {
                additional.right += compensation
            }
            if current.view.directionalLayoutMargins != margins {
                current.view.directionalLayoutMargins = margins
            }
            if current.additionalSafeAreaInsets != additional {
                current.additionalSafeAreaInsets = additional
            }
        }
    }

    private func restoreNavigationLayout() {
        guard isCorrecting, let navigation else { return }
        isCorrecting = false
        UIView.performWithoutAnimation {
            navigation.viewRespectsSystemMinimumLayoutMargins = originalRespectsSystemMargins
            navigation.view.directionalLayoutMargins = originalMargins
            navigation.view.insetsLayoutMarginsFromSafeArea = originalInsetsMarginsFromSafeArea
            navigation.additionalSafeAreaInsets = originalAdditionalInsets
        }
    }

    func stopObservingNavigationLayout() {
        isUpdating = true
        defer { isUpdating = false }
        safeAreaObserver.onChange = nil
        safeAreaObserver.removeFromSuperview()
        restoreNavigationLayout()
    }

    private func recordBaseline(_ navigation: UINavigationController) {
        let safe = navigation.view.safeAreaInsets
        contentMargins = navigation.view.directionalLayoutMargins
        contentMargins.top -= safe.top
        contentMargins.bottom -= safe.bottom
        contentMargins.leading -= layoutDirection == .leftToRight ? safe.left : safe.right
        contentMargins.trailing -= layoutDirection == .leftToRight ? safe.right : safe.left
    }

    private func firstNavigationController(in controller: UIViewController) -> UINavigationController? {
        if let navigation = controller as? UINavigationController { return navigation }
        for child in controller.children {
            if let navigation = firstNavigationController(in: child) { return navigation }
        }
        return nil
    }
}

/// Older UIKit versions update the navigation view's inherited safe area after its parent's
/// layout callback. Observe that view directly so compensation happens in the same layout pass.
private final class DrawerSafeAreaObserver: UIView {
    var onChange: (() -> Void)?

    override func safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange()
        onChange?()
    }
}
