import SwiftUI

/// Pushes the modified view toward the trailing edge to reveal a drawer beneath it.
///
/// One normalized reveal progress drives every visual property: the drawer content's scale
/// and opacity, and the pushed screen's offset, dimming, and leading corners. Requests animate
/// that progress toward an endpoint, and a drag takes it over from the value on screen, so
/// reversing or grabbing a transition midway continues from where it is.
///
/// One horizontal drag over the whole container drives the reveal: from anywhere on the screen
/// while the drawer is closed, and from anywhere on the drawer or the pushed screen otherwise.
struct DrawerContainer<Base: View, DrawerContent: View>: View {
    @Binding var isShowing: Bool
    let allowsDragging: Bool
    let base: Base
    let drawerContent: () -> DrawerContent

    @Environment(\.drawerStyle) private var style
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var reveal: DrawerReveal
    /// The vertical band, in global coordinates, of a tab bar that keeps its own drags.
    @State private var excludedBand: ClosedRange<CGFloat>?
    @AccessibilityFocusState private var focus: DrawerFocus?

    init(
        isShowing: Binding<Bool>,
        allowsDragging: Bool = true,
        base: Base,
        drawerContent: @escaping () -> DrawerContent,
        reveal: DrawerReveal? = nil,
        excludedBand: ClosedRange<CGFloat>? = nil
    ) {
        self._isShowing = isShowing
        self.allowsDragging = allowsDragging
        self.base = base
        self.drawerContent = drawerContent
        self._reveal = State(initialValue: reveal ?? DrawerReveal(isOpen: isShowing.wrappedValue))
        self._excludedBand = State(initialValue: excludedBand)
    }

    var body: some View {
        GeometryReader { geometry in
            let layout = DrawerLayout(size: geometry.size, safeAreaInsets: geometry.safeAreaInsets)

            ZStack(alignment: .leading) {
                drawer(in: layout)
                foreground(in: layout)
                // Above the pushed screen rather than inside it, so blocking the screen can
                // never block dismissal.
                DrawerDismissalSurface(
                    progress: reveal.progress,
                    reveal: reveal,
                    travel: layout.width,
                    onDismiss: dismiss
                )
            }
            // On the container rather than on the screen or the drawer, so the drag keeps its
            // touch when the reveal blocks the screen midway.
            .modifier(DrawerDragGesture(
                progress: reveal.progress,
                isEnabled: allowsDragging,
                holdsDrag: reveal.drag != nil,
                handler: dragHandler(width: layout.width)
            ))
            .ignoresSafeArea()
        }
        .environment(\.drawerFocus, $focus)
        .environment(\.drawerExcludedBandReporter, DrawerExcludedBandReporter { band in
            if excludedBand != band {
                excludedBand = band
            }
        })
        .onChange(of: isShowing) { isOpen in
            if !reveal.isResting(isOpen: isOpen) {
                animateReveal(toOpen: isOpen)
            }
        }
    }

    private func drawer(in layout: DrawerLayout) -> some View {
        style.makeBody(configuration: DrawerStyleConfiguration(
            content: DrawerStyleConfiguration.Content(view: AnyView(
                DrawerScrollView(
                    minHeight: layout.viewportHeight,
                    onEscape: dismiss,
                    content: drawerContent
                )
                    .padding(layout.contentInsets)
                    .modifier(DrawerContentReveal(
                        progress: reveal.progress,
                        reveal: reveal,
                        reduceMotion: reduceMotion,
                        onSettle: didSettle(at:)
                    ))
            ))
        ))
        .frame(width: layout.width)
        .frame(maxHeight: .infinity)
        .modifier(DrawerSurfaceInteraction(progress: reveal.progress, reveal: reveal))
    }

    private func foreground(in layout: DrawerLayout) -> some View {
        base
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .modifier(DrawerForegroundReveal(
                progress: reveal.progress,
                reveal: reveal,
                travel: layout.width
            ))
    }

    private func dragHandler(width: CGFloat) -> DrawerDragHandler {
        DrawerDragHandler(
            onChange: { value, presentedProgress, startsTouch in
                var next = reveal
                // An earlier touch's drag is normally finished once a render observes that the
                // touch ended. If a touch starts before that happened, the leftover drag must
                // not decide this touch: it is finished as a cancellation first. The drag held
                // here can also be this touch's own, when SwiftUI delivered several updates
                // before rendering; deciding afresh from the whole translation then reaches the
                // same decision unless the finger turned in between, and continues from the
                // reveal on screen.
                let abandonedEndpoint = startsTouch ? next.cancelDrag() : nil
                let isClaimed = next.updateDrag(
                    translation: logical(value.translation),
                    presentedProgress: presentedProgress,
                    width: width,
                    startsInExcludedRegion: excludedBand?.contains(value.startLocation.y) ?? false
                )
                // Writing the presented value without animation replaces any in-flight
                // transition, so the drag continues from what is on screen.
                var transaction = Transaction()
                transaction.disablesAnimations = true
                withTransaction(transaction) {
                    reveal = next
                }
                if let abandonedEndpoint, !next.isTracking {
                    returnToSettledEndpoint(isOpen: abandonedEndpoint)
                }
                return isClaimed
            },
            onEnd: { value in
                guard let isOpen = reveal.endDrag(
                    predictedTranslation: logical(value.predictedEndTranslation).width,
                    width: width
                ) else { return }

                animateReveal(toOpen: isOpen)
                isShowing = isOpen
            },
            onFinish: {
                if let isOpen = reveal.cancelDrag() {
                    returnToSettledEndpoint(isOpen: isOpen)
                }
            }
        )
    }

    /// A cancelled drag returns to where the drawer last rested, and the binding follows, even
    /// if a request was heading for the other endpoint when the drag took over.
    private func returnToSettledEndpoint(isOpen: Bool) {
        animateReveal(toOpen: isOpen)
        isShowing = isOpen
    }

    private func logical(_ translation: CGSize) -> CGSize {
        DrawerReveal.logicalTranslation(translation, layoutDirection: layoutDirection)
    }

    /// Reduce Motion reaches the endpoint immediately: animating only the fade would still
    /// slide the pushed screen, because its offset is animatable in the same transaction.
    private func animateReveal(toOpen isOpen: Bool) {
        withAnimation(reduceMotion ? nil : revealAnimation) {
            reveal.settle(isOpen: isOpen)
        }
    }

    private func dismiss() {
        isShowing = false
    }

    /// Records where the reveal came to rest and moves VoiceOver focus there. A phase change
    /// can arrive after the model has already moved on — a drag cancelled in the same update,
    /// for example — so only an endpoint the model still rests at counts.
    private func didSettle(at phase: DrawerPhase) {
        switch phase {
        case .open where reveal.isResting(isOpen: true):
            reveal.noteSettled(isOpen: true)
            focus = .content
        case .closed where reveal.isResting(isOpen: false):
            reveal.noteSettled(isOpen: false)
            focus = .opener
        default:
            break
        }
    }
}

/// Reveals the drawer content as one group: it grows from a slightly reduced scale and fades
/// in with the reveal, keeping its final layout so text never reflows while it grows.
private struct DrawerContentReveal: ViewModifier, Animatable {
    var progress: CGFloat
    let reveal: DrawerReveal
    let reduceMotion: Bool
    let onSettle: (DrawerPhase) -> Void

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func body(content: Content) -> some View {
        let phase = DrawerPhase(presentedProgress: progress, reveal: reveal)
        let visibility = progress.clampedToUnit
        // Reduce Motion also keeps a drag free of decorative scaling.
        let scale = reduceMotion ? 1 : closedContentScale + (1 - closedContentScale) * visibility

        content
            .scaleEffect(scale, anchor: .leading)
            .opacity(visibility)
            // Actions respond, and reach assistive technology, only once the drawer has
            // settled open.
            .allowsHitTesting(phase == .open)
            .accessibilityHidden(phase != .open)
            .onChange(of: phase) { settledPhase in
                onSettle(settledPhase)
            }
    }
}

/// Makes the whole visible drawer, including its empty space, a place where the container's
/// drag can start, and keeps the hidden drawer from taking touches.
private struct DrawerSurfaceInteraction: ViewModifier, Animatable {
    var progress: CGFloat
    let reveal: DrawerReveal

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func body(content: Content) -> some View {
        let phase = DrawerPhase(presentedProgress: progress, reveal: reveal)

        content
            .contentShape(Rectangle())
            .allowsHitTesting(phase.isModal)
    }
}

/// Pushes the foreground aside with the reveal, dimming it and rounding its leading corners,
/// and blocks it whenever the drawer is not closed.
private struct DrawerForegroundReveal: ViewModifier, Animatable {
    var progress: CGFloat
    let reveal: DrawerReveal
    let travel: CGFloat

    @Environment(\.colorScheme) private var colorScheme

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func body(content: Content) -> some View {
        let phase = DrawerPhase(presentedProgress: progress, reveal: reveal)
        let visibility = progress.clampedToUnit
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: foregroundCornerRadius * visibility,
            bottomLeadingRadius: foregroundCornerRadius * visibility
        )

        content
            .allowsHitTesting(!phase.isModal)
            .accessibilityHidden(phase.isModal)
            .overlay {
                Color.black
                    .opacity(scrimOpacity * visibility)
                    .allowsHitTesting(false)
            }
            .clipShape(shape)
            .overlay {
                shape
                    .strokeBorder(.quaternary, lineWidth: edgeLineWidth)
                    .opacity(visibility)
                    .allowsHitTesting(false)
            }
            .offset(x: travel * visibility)
    }

    private var scrimOpacity: CGFloat {
        colorScheme == .dark ? darkScrimOpacity : lightScrimOpacity
    }
}

/// Covers the pushed screen, moving with it, whenever the drawer is not closed. Tapping its
/// visible strip, or activating it with assistive technology, closes the drawer; a drag on it
/// belongs to the container's drag.
private struct DrawerDismissalSurface: View, Animatable {
    var progress: CGFloat
    let reveal: DrawerReveal
    let travel: CGFloat
    let onDismiss: () -> Void

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    var body: some View {
        if DrawerPhase(presentedProgress: progress, reveal: reveal).isModal {
            Button(action: onDismiss) {
                Color.clear
                    .contentShape(Rectangle())
            }
            .buttonStyle(DismissalSurfaceButtonStyle())
            .accessibilityLabel(Text(.closeMenuAction))
            .accessibilityAction(.escape, onDismiss)
            // The surface spans the whole pushed screen, so it would otherwise read before the
            // drawer's content.
            .accessibilitySortPriority(-1)
            .offset(x: travel * progress.clampedToUnit)
        }
    }
}

private struct DismissalSurfaceButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
    }
}

struct DrawerDragHandler {
    /// Returns `false` when the drawer declines the drag. `startsTouch` is `true` while the
    /// gesture state shows no active touch: on a touch's first update, and on any further
    /// updates SwiftUI delivers before its next render, which also start from that state.
    let onChange: (DragGesture.Value, _ presentedProgress: CGFloat, _ startsTouch: Bool) -> Bool
    let onEnd: (DragGesture.Value) -> Void
    /// Runs whenever the gesture stops: after `onEnd` on release, alone on cancellation.
    let onFinish: () -> Void
}

/// Feeds a horizontal drag to the reveal with the progress on screen at the time, measured in
/// the window's coordinates so moving the dragged surface never feeds back into the drag.
///
/// The drag has high priority over everything in the container, so once it is recognized the
/// control where it started — a drawer row, a screen row, the menu opener — cannot also
/// activate; a tap, which never travels far enough to start the drag, still activates it.
/// Scroll views keep vertical drags because they recognize them within a shorter distance.
/// When the drawer declines a drag, the gesture detaches until that touch ends, which hands the
/// touch back to the views beneath, such as a tab bar sliding between tabs.
struct DrawerDragGesture: ViewModifier, Animatable {
    var progress: CGFloat
    /// While `false`, the gesture is detached rather than ignored, so it cannot compete with
    /// a destination's back button and back-swipe.
    let isEnabled: Bool
    /// Whether the reveal model is holding a drag, claimed or declined.
    let holdsDrag: Bool
    let handler: DrawerDragHandler

    /// Gesture state, so SwiftUI resets it whenever the touch ends, is cancelled, or detaches:
    /// a declined touch can never leave the gesture detached for the touches after it.
    @GestureState private var touch = DrawerTouch()

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func body(content: Content) -> some View {
        let gesture = DragGesture(minimumDistance: dragMinimumDistance, coordinateSpace: .global)
            .updating($touch) { value, touch, _ in
                let startsTouch = !touch.isActive
                touch.isActive = true
                if !touch.isDeclined, !handler.onChange(value, progress, startsTouch) {
                    touch.isDeclined = true
                }
            }
            .onEnded { handler.onEnd($0) }

        content
            .highPriorityGesture(gesture, including: isEnabled && !touch.isDeclined ? .all : .subviews)
            .onChange(of: touch.isActive) { isActive in
                if !isActive {
                    handler.onFinish()
                }
            }
            // A touch that ends or is cancelled before the next render never shows up as
            // active above, so a drag it left in the model is finished here instead.
            .onChange(of: holdsDrag) { holdsDrag in
                if holdsDrag, !touch.isActive {
                    handler.onFinish()
                }
            }
    }
}

/// The touch the drawer's drag is following.
struct DrawerTouch: Equatable {
    var isActive = false
    /// The drawer declined the drag, so the gesture stays detached for the rest of the touch.
    var isDeclined = false
}

/// Scrolls the drawer content when it outgrows the window and otherwise stretches it to the
/// window's height, so a trailing footer rests at the bottom.
private struct DrawerScrollView<Content: View>: View {
    let minHeight: CGFloat
    let onEscape: () -> Void
    let content: () -> Content

    var body: some View {
        ScrollView(.vertical) {
            content()
                .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .top)
                // The escape action must sit inside the scroll view: VoiceOver walks up from
                // the focused element through platform containers, and the scroll view's
                // ancestors in SwiftUI are not among them.
                .accessibilityElement(children: .contain)
                .accessibilityAction(.escape, onEscape)
        }
        .modifier(ScrollBounceBasedOnSize())
        // Keeps scrolled content out of the status bar and home indicator areas, which the
        // drawer surface extends behind.
        .clipped()
    }
}

private struct ScrollBounceBasedOnSize: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.4, *) {
            content.scrollBounceBehavior(.basedOnSize)
        } else {
            content
        }
    }
}

/// Where the drawer sends VoiceOver focus once a transition settles.
enum DrawerFocus: Hashable {
    case opener
    case content
}

private struct DrawerFocusKey: EnvironmentKey {
    static let defaultValue: AccessibilityFocusState<DrawerFocus?>.Binding? = nil
}

extension EnvironmentValues {
    var drawerFocus: AccessibilityFocusState<DrawerFocus?>.Binding? {
        get { self[DrawerFocusKey.self] }
        set { self[DrawerFocusKey.self] = newValue }
    }
}

/// Lets a host report the vertical band of a tab bar that keeps its own drags.
struct DrawerExcludedBandReporter {
    let report: (_ band: ClosedRange<CGFloat>?) -> Void

    init(_ report: @escaping (_ band: ClosedRange<CGFloat>?) -> Void) {
        self.report = report
    }
}

private struct DrawerExcludedBandReporterKey: EnvironmentKey {
    static let defaultValue: DrawerExcludedBandReporter? = nil
}

private struct DrawerTabViewSafeFrameKey: EnvironmentKey {
    static let defaultValue: CGRect? = nil
}

extension EnvironmentValues {
    var drawerExcludedBandReporter: DrawerExcludedBandReporter? {
        get { self[DrawerExcludedBandReporterKey.self] }
        set { self[DrawerExcludedBandReporterKey.self] = newValue }
    }

    /// The safe-area frame of a tab view whose bar is excluded from drawer drags.
    var drawerTabViewSafeFrame: CGRect? {
        get { self[DrawerTabViewSafeFrameKey.self] }
        set { self[DrawerTabViewSafeFrameKey.self] = newValue }
    }
}

/// Where a tab bar sits, from what SwiftUI reports: the bar is part of each tab's safe-area
/// inset but not of the tab view's, so it fills the gap between the two safe frames.
enum DrawerTabBarBand {
    /// Both frames are global. SwiftUI reports where a bar begins but not where it ends, so a
    /// bar below the content, as on iPhone, is taken to reach the screen edge. On iOS 18 it
    /// does; on iOS 26 and later its floating platter extends into the home indicator's inset by
    /// an amount SwiftUI does not report, so the band also covers the host content beside and
    /// below the platter. A bar above the content, as on iPad in regular width, spans from the
    /// tab view's safe top (below the navigation bar) to the content.
    static func band(tabViewSafeFrame: CGRect, tabContentSafeFrame content: CGRect) -> ClosedRange<CGFloat>? {
        guard tabViewSafeFrame.height > 0, content.height > 0 else { return nil }

        if content.maxY < tabViewSafeFrame.maxY - tolerance {
            return content.maxY...CGFloat.greatestFiniteMagnitude
        }
        if content.minY > tabViewSafeFrame.minY + tolerance {
            return tabViewSafeFrame.minY...content.minY
        }
        return nil
    }

    private static let tolerance: CGFloat = 1
}

/// Measures the tab view's safe frame and hands it to the tab roots below it.
private struct DrawerTabBarExclusion: ViewModifier {
    @State private var safeFrame: CGRect?

    func body(content: Content) -> some View {
        content
            .environment(\.drawerTabViewSafeFrame, safeFrame)
            .background {
                GeometryReader { geometry in
                    let frame = geometry.frame(in: .global)

                    Color.clear
                        .onAppear { safeFrame = frame }
                        .onChange(of: frame) { safeFrame = $0 }
                }
            }
    }
}

/// Measures a tab root's safe frame, which ends at the tab bar, and reports the bar's band.
private struct DrawerTabBarBoundary: ViewModifier {
    @Environment(\.drawerTabViewSafeFrame) private var tabViewSafeFrame
    @Environment(\.drawerExcludedBandReporter) private var reporter

    func body(content: Content) -> some View {
        content.background {
            GeometryReader { geometry in
                let band = tabViewSafeFrame.flatMap {
                    DrawerTabBarBand.band(tabViewSafeFrame: $0, tabContentSafeFrame: geometry.frame(in: .global))
                }

                Color.clear
                    .onAppear { reporter?.report(band) }
                    .onChange(of: band) { reporter?.report($0) }
            }
        }
    }
}

private struct DrawerFocusAnchor: ViewModifier {
    let target: DrawerFocus

    @Environment(\.drawerFocus) private var focus

    @ViewBuilder
    func body(content: Content) -> some View {
        if let focus {
            content.accessibilityFocused(focus, equals: target)
        } else {
            content
        }
    }
}

public extension View {
    /// Pushes this view aside to reveal a drawer. A horizontal drag toward the trailing edge
    /// opens it from anywhere on this view, and one toward the leading edge closes it from
    /// anywhere on the drawer or the pushed view.
    ///
    /// - Parameter allowsDragging: Whether drags move the drawer. A host passes `false` from
    ///   the moment it shows a destination, including while the drawer finishes closing, so the
    ///   destination's back button and back-swipe keep their gestures. A host that keeps a
    ///   `DrawerHostNavigation` passes its `isHostVisible`.
    func drawer<Content: View>(
        isShowing: Binding<Bool>,
        allowsDragging: Bool = true,
        @ViewBuilder content: @escaping () -> Content,
    ) -> some View {
        DrawerContainer(
            isShowing: isShowing,
            allowsDragging: allowsDragging,
            base: self,
            drawerContent: content
        )
    }

    /// Leaves drags that start on this tab view's tab bar, which switches tabs as a finger
    /// slides across it, to the bar while the enclosing drawer is closed. The bar is found
    /// wherever the system places it — at the bottom, or at the top on iPad in regular width —
    /// from the root views of the tabs, which must each be marked with `drawerTabBarBoundary()`.
    func excludesTabBarFromDrawerDrags() -> some View {
        modifier(DrawerTabBarExclusion())
    }

    /// Marks a tab's root view, whose safe area ends at the tab bar, so the enclosing
    /// `excludesTabBarFromDrawerDrags()` can locate the bar.
    func drawerTabBarBoundary() -> some View {
        modifier(DrawerTabBarBoundary())
    }

    /// Marks the control that opens the enclosing drawer. VoiceOver focus returns to it once
    /// the drawer finishes closing, provided the control is still on screen.
    func drawerOpener() -> some View {
        modifier(DrawerFocusAnchor(target: .opener))
    }

    /// Marks the drawer element VoiceOver focuses once the drawer finishes opening.
    func drawerInitialFocus() -> some View {
        modifier(DrawerFocusAnchor(target: .content))
    }
}

private let closedContentScale: CGFloat = 0.95
private let foregroundCornerRadius: CGFloat = 24
private let lightScrimOpacity: CGFloat = 0.18
private let darkScrimOpacity: CGFloat = 0.24
private let edgeLineWidth: CGFloat = 1
/// Longer than the distance within which a scroll view recognizes its own drag (measured between
/// 10 and 15 points on iOS 27), so a vertical drag starts scrolling before the drawer's
/// high-priority drag is recognized. Recognized first, the drawer's drag would hold the scroll
/// view back.
private let dragMinimumDistance: CGFloat = 20
/// Critically damped, so the reveal settles without overshooting either endpoint.
private let revealAnimation = Animation.spring(response: 0.35, dampingFraction: 1)

private struct DrawerPreviewScreen: View {
    var body: some View {
        NavigationStack {
            List(1..<30) { index in
                Text("Row \(index)")
            }
            .navigationTitle("Screen")
        }
    }
}

private struct DrawerPreviewMenu: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Account")
                .font(.headline)
            Text("Profile")
            Spacer()
            Text("Sign out")
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#Preview("Closed") {
    DrawerPreviewScreen()
        .drawer(isShowing: .constant(false)) { DrawerPreviewMenu() }
}

#Preview("Half open") {
    DrawerContainer(
        isShowing: .constant(true),
        base: DrawerPreviewScreen(),
        drawerContent: { DrawerPreviewMenu() },
        reveal: DrawerReveal(heldAt: 0.5)
    )
}

#Preview("Open") {
    DrawerPreviewScreen()
        .drawer(isShowing: .constant(true)) { DrawerPreviewMenu() }
}

#Preview("Open dark") {
    DrawerPreviewScreen()
        .drawer(isShowing: .constant(true)) { DrawerPreviewMenu() }
        .preferredColorScheme(.dark)
}

#Preview("Open right-to-left") {
    DrawerPreviewScreen()
        .drawer(isShowing: .constant(true)) { DrawerPreviewMenu() }
        .environment(\.layoutDirection, .rightToLeft)
}
