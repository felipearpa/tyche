package com.felipearpa.tyche.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.sign

/**
 * Pushes [content] toward the trailing edge to reveal a drawer beneath it.
 *
 * The caller owns whether the drawer is open: [isOpen] is the endpoint it asks for, and
 * [onOpenChange] reports an endpoint the drawer asks for by itself, after a drag, Back, Escape, or
 * a tap or accessibility click on the pushed screen. One normalized reveal progress drives every visual property: the drawer
 * content's scale and opacity, and the pushed screen's offset, dimming, leading corners, and edge.
 * Requests animate that progress from its value on screen, and a drag takes it over from that same
 * value, so reversing or grabbing a transition midway continues from where it is.
 *
 * A predominantly horizontal drag moves the drawer: toward the trailing edge from anywhere while it
 * is closed, and either way from anywhere on the drawer or the pushed screen otherwise. Once the
 * drawer recognizes a drag, the control where it started does not activate. Descendants that
 * handle their own drags, such as scroll views, keep them, and edges the system reserves for its
 * own gestures, such as Back under gesture navigation, are left to the system.
 *
 * The drawer learns that the caller closed it only when the caller recomposes, so its actions still
 * respond until then; a drawer action that leaves the caller's route should run through
 * [runIfStarted], which keeps a second activation in that window from leaving again.
 */
@Composable
fun PushDrawer(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PushDrawer(
        isOpen = isOpen,
        onOpenChange = onOpenChange,
        drawerContent = drawerContent,
        reveal = remember { DrawerReveal(isOpen) },
        modifier = modifier,
        content = content,
    )
}

/**
 * [reveal] lets previews and tests start from a given progress; [systemGestureInsets] are the
 * window edges where the drawer leaves drags to the system.
 */
@Composable
internal fun PushDrawer(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    reveal: DrawerReveal,
    modifier: Modifier = Modifier,
    systemGestureInsets: WindowInsets = WindowInsets.systemGestures,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentIsOpen by rememberUpdatedState(isOpen)
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)
    val phase by remember(reveal) { derivedStateOf { reveal.phase } }
    // The accessible dismissal joins the drawer content once the drawer settles open, not while it
    // opens: TalkBack moves its cursor off the vanished opener to the first node it can reach, and
    // with nothing to reach while the drawer opens, that is the drawer's first element once it
    // settles, ahead of the dismissal in traversal order. It stays until the drawer closes.
    val exposesDismissal by remember(reveal) {
        derivedStateOf { reveal.phase == DrawerPhase.Open || (reveal.phase.isModal && reveal.settledIsOpen) }
    }

    // Counts the endpoints the drawer proposed after a drag or Back, so the effect below also runs
    // when the caller keeps `isOpen` unchanged and the drawer must settle back to it.
    var proposalCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(reveal, isOpen, proposalCount) {
        if (reveal.targetIsOpen != isOpen) reveal.animateTo(isOpen)
    }

    val propose: (Boolean, Float) -> Unit = remember(reveal, scope) {
        { proposedIsOpen, velocity ->
            // Undispatched, so this transition replaces any other before the caller answers.
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                reveal.animateTo(proposedIsOpen, velocity)
            }
            if (proposedIsOpen != currentIsOpen) currentOnOpenChange(proposedIsOpen)
            proposalCount++
        }
    }

    // Asks the caller to close; while the drawer is already closing, the tap has nothing to add.
    val dismiss: () -> Unit = remember { { if (currentIsOpen) currentOnOpenChange(false) } }

    DrawerBackHandler(isEnabled = phase.isModal, reveal = reveal, onSettle = propose)

    val drawerFocus = remember { FocusRequester() }
    val foregroundFocus = remember { FocusRequester() }
    val focusMemory = remember { DrawerFocusMemory() }
    DrawerFocusTransitions(
        phase = phase,
        isOpenRequested = isOpen,
        drawerFocus = drawerFocus,
        foregroundFocus = foregroundFocus,
        focusMemory = focusMemory,
    )

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val hasDecorativeScale = rememberAnimationsEnabled()
    val scrimAlpha =
        if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DarkScrimAlpha else LightScrimAlpha
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    val closeMenuLabel = stringResource(R.string.close_menu_action)
    val systemGestureArea = rememberSystemGestureArea(systemGestureInsets)
    // The drawer surface extends behind the system bars and a display cutout; its content keeps
    // clear of them on the sides it touches.
    val drawerInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val drawerInsetPadding = drawerInsets.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val drawerWidth = drawerWidth(
            maxWidth = maxWidth,
            leadingInset = drawerInsetPadding.calculateStartPadding(layoutDirection),
            trailingInset = drawerInsetPadding.calculateEndPadding(layoutDirection),
        )
        // Physical pixels the pushed screen moves per unit of progress; negative right-to-left.
        val travel = with(density) { drawerWidth.toPx() } * if (isRtl) -1f else 1f
        val currentTravel by rememberUpdatedState(travel)

        Box(
            Modifier
                .fillMaxSize()
                .onPlaced { systemGestureArea.coordinates = it }
                // Escape from the focused drawer does what Back does, before focus handling
                // would take it to leave the focused control.
                .onKeyEvent { event ->
                    val isEscape = event.key == Key.Escape && phase.isModal
                    if (isEscape && event.type == KeyEventType.KeyDown) propose(false, 0f)
                    isEscape
                }
                .semantics { isTraversalGroup = true }
                // On the container rather than on the drawer or the pushed screen, so a drag keeps
                // its touch when the reveal blocks the pushed screen midway.
                .pointerInput(reveal) {
                    detectDrawerDrags(
                        reveal = reveal,
                        travel = { currentTravel },
                        startsInSystemGestureArea = systemGestureArea::contains,
                        onRelease = propose,
                    )
                },
        ) {
            Box(
                Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    // No pane title: its appearance would make TalkBack restore the node it last
                    // focused in that pane, such as "Close menu", instead of the drawer's first
                    // element.
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = 0f
                    },
            ) {
                // Closed, the content stays composed, so its state and data survive, but it is
                // not placed: it is not drawn, touched, focused, or read by assistive technology.
                PlacedWhile(isPlaced = { phase != DrawerPhase.Closed }) {
                    DrawerContentGroup(
                        reveal = reveal,
                        phase = phase,
                        hasDecorativeScale = hasDecorativeScale,
                        isRtl = isRtl,
                        insets = drawerInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                        // Refused from the frame the caller asks the drawer to close, so the focus
                        // cleared then cannot come straight back.
                        acceptsFocus = phase == DrawerPhase.Open && isOpen,
                        focusRequester = drawerFocus,
                        onHasFocusChange = { focusMemory.drawerHasFocus = it },
                        content = drawerContent,
                    )
                }

                if (phase != DrawerPhase.Open) {
                    InputBlocker(Modifier.matchParentSize())
                }
            }

            if (phase.isModal) {
                // Drawn above the pushed screen, so blocking the pushed screen never blocks
                // dismissal. Like Material's drawer scrim, it answers taps and accessibility
                // clicks but is no keyboard focus stop: Back and Escape dismiss from the keyboard,
                // and focus never sits on a surface that disappears once the drawer closes. Taps
                // dismiss throughout; while the drawer first opens, Back is the accessible
                // dismissal.
                Box(
                    Modifier
                        .zIndex(1f)
                        .fillMaxSize()
                        .graphicsLayer { translationX = travel * reveal.progress }
                        .pointerInput(Unit) { detectTapGestures { dismiss() } }
                        .then(
                            if (exposesDismissal) {
                                Modifier.semantics {
                                    traversalIndex = 1f
                                    contentDescription = closeMenuLabel
                                    role = Role.Button
                                    onClick {
                                        dismiss()
                                        true
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val progress = reveal.progress
                        val radius = ForegroundCornerRadius.toPx() * progress
                        translationX = travel * progress
                        shape = RoundedCornerShape(topStart = radius, bottomStart = radius)
                        clip = progress > 0f
                    }
                    .focusRequester(foregroundFocus)
                    // Farther from the focus group than the restorer, so it overrides the
                    // restorer's entry while the drawer is modal.
                    .focusProperties {
                        if (phase.isModal) onEnter = { cancelFocusChange() }
                    }
                    .focusRestorer()
                    .onFocusChanged { focusMemory.foregroundHasFocus = it.hasFocus }
                    .focusGroup()
                    .then(if (phase.isModal) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                content()

                // Separate layers, so animating them never redraws the pushed screen.
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = scrimAlpha * reveal.progress }
                        .background(Color.Black),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer()
                        .drawBehind {
                            val progress = reveal.progress
                            if (progress > 0f) {
                                val radius = ForegroundCornerRadius.toPx() * progress
                                drawOutline(
                                    outline = RoundedCornerShape(topStart = radius, bottomStart = radius)
                                        .createOutline(size, layoutDirection, this),
                                    color = edgeColor,
                                    alpha = progress,
                                    style = Stroke(width = EdgeWidth.toPx()),
                                )
                            }
                        },
                )
            }
        }
    }
}

/**
 * Hands Back to the drawer while it is modal, so Back dismisses it before navigating away. A
 * predictive Back gesture moves the reveal with its progress, and cancelling the gesture returns
 * the drawer to where it last rested.
 */
@Composable
private fun DrawerBackHandler(
    isEnabled: Boolean,
    reveal: DrawerReveal,
    onSettle: (isOpen: Boolean, velocity: Float) -> Unit,
) {
    PredictiveBackHandler(enabled = isEnabled) { events ->
        var drag: DrawerReveal.Drag? = null
        try {
            events.collect { event ->
                val activeDrag = drag ?: reveal.startDrag().also { drag = it }
                activeDrag.translateTo(-activeDrag.origin * event.progress)
            }
        } catch (cancellation: CancellationException) {
            if (drag?.isActive == true) onSettle(reveal.settledIsOpen, 0f)
            throw cancellation
        }
        onSettle(false, 0f)
    }
}

/**
 * Keeps keyboard focus where the drawer takes input: in the drawer only while it is open, and on
 * the pushed screen once it is closed.
 *
 * Focus moves into the drawer once it settles open, and again whenever keyboard input begins while
 * it is open, such as a Tab after a touch opened it. Without keyboard input the drawer's controls
 * cannot take focus, so focus left on the pushed screen, such as a text field's, is cleared
 * instead.
 *
 * As soon as the caller asks the drawer to close, or the drawer leaves the open endpoint by itself,
 * focus held anywhere in it is cleared, however it got there, because a focused control keeps
 * receiving keys even when it is hidden. Once the drawer settles closed, that focus returns to the
 * pushed screen: to the control that held it before, the opener after a keyboard opening, or else
 * to the pushed screen's default entry, since a touch opening leaves no focused control behind. The
 * return waits while the host route is not resumed: an activity pause, such as Invite's share
 * sheet, leaves the route active and focus returns once it resumes, while a route that navigated
 * away is disposed first and takes no focus back.
 */
@Composable
private fun DrawerFocusTransitions(
    phase: DrawerPhase,
    isOpenRequested: Boolean,
    drawerFocus: FocusRequester,
    foregroundFocus: FocusRequester,
    focusMemory: DrawerFocusMemory,
) {
    val focusManager = LocalFocusManager.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val inputMode = LocalInputModeManager.current.inputMode

    fun releaseDrawerFocus() {
        if (focusMemory.drawerHasFocus) {
            focusMemory.returnsOnClose = true
            focusManager.clearFocus(force = true)
        }
    }

    // The caller's request reaches this composition a frame before the phase it starts, so a key
    // pressed right after the one that chose a destination no longer reaches that action.
    LaunchedEffect(isOpenRequested) {
        if (!isOpenRequested) releaseDrawerFocus()
    }

    // Keyed on the composed phase, so the focus properties of that phase already apply: while the
    // drawer is not open, neither it nor the pushed screen lets focus enter. While it is open, also
    // keyed on the input mode: the system's own first focus search after touch input can pick a
    // control of the pushed screen, whose entry is refused, and would leave focus nowhere.
    LaunchedEffect(phase, inputMode.takeIf { phase == DrawerPhase.Open }) {
        when (phase) {
            DrawerPhase.Open -> {
                focusMemory.returnsOnClose = false
                // Focus already in the drawer stays on its control.
                if (!focusMemory.drawerHasFocus) {
                    val drawerTookFocus = drawerFocus.requestFocus(FocusDirection.Enter)
                    if (!drawerTookFocus && focusMemory.foregroundHasFocus) {
                        focusManager.clearFocus()
                    }
                }
            }

            DrawerPhase.Closed -> {
                // Also reached straight from the open endpoint when animations are off.
                if (focusMemory.drawerHasFocus) focusMemory.returnsOnClose = true
                if (focusMemory.returnsOnClose) {
                    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        releaseDrawerFocus()
                        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
                    }
                    foregroundFocus.requestFocus(FocusDirection.Enter)
                    // Should the pushed screen have nothing to focus, the hidden drawer still
                    // gives its focus up.
                    releaseDrawerFocus()
                    focusMemory.returnsOnClose = false
                }
            }

            DrawerPhase.Opening, DrawerPhase.Closing, DrawerPhase.Dragging -> releaseDrawerFocus()
        }
    }
}

private class DrawerFocusMemory {
    var foregroundHasFocus = false
    var drawerHasFocus = false

    /** Whether keyboard focus returns to the pushed screen once the drawer settles closed. */
    var returnsOnClose = false
}

/**
 * Feeds horizontal drags to the reveal. [travel] is the physical distance for the whole reveal,
 * negative right-to-left. [onRelease] receives the endpoint to settle at and the release velocity
 * in progress per second.
 *
 * The detector sees each event after the descendants, so a descendant that handles its own drags
 * claims it first. The drawer decides once the finger passes touch slop along either axis, the
 * way descendant scroll views measure it: a predominantly vertical drag is left alone; a horizontal
 * one is consumed, which cancels the click of the control under the finger, and moves the reveal
 * unless the drawer rests closed and the drag points toward the leading edge.
 */
private suspend fun PointerInputScope.detectDrawerDrags(
    reveal: DrawerReveal,
    travel: () -> Float,
    startsInSystemGestureArea: (Offset) -> Boolean,
    onRelease: (isOpen: Boolean, velocity: Float) -> Unit,
) {
    val velocityTracker = VelocityTracker()
    val maximumVelocity = viewConfiguration.maximumFlingVelocity.let { Velocity(it, it) }

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (startsInSystemGestureArea(down.position)) return@awaitEachGesture

        velocityTracker.resetTracking()
        velocityTracker.addPointerInputChange(down)
        val claim = awaitClaimedDrag(
            down = down,
            logicalSign = { sign(travel()) },
            restsClosed = { reveal.phase == DrawerPhase.Closed },
            onMove = velocityTracker::addPointerInputChange,
        ) ?: return@awaitEachGesture

        val revealDrag = reveal.startDrag()
        var progressTranslation = 0f
        // Measured against the drawer width at each move, so a resize keeps the progress.
        fun moveBy(physicalX: Float) {
            val currentTravel = travel()
            if (currentTravel != 0f) {
                progressTranslation += physicalX / currentTravel
                revealDrag.translateTo(progressTranslation)
            }
        }
        moveBy(claim.overSlop)
        val isReleased = try {
            drag(claim.change.id) { change ->
                velocityTracker.addPointerInputChange(change)
                moveBy(change.positionChange().x)
                change.consume()
            }
        } catch (cancellation: CancellationException) {
            // The detector stopped with the finger down, so nothing else would end the drag.
            if (revealDrag.isActive) onRelease(reveal.settledIsOpen, 0f)
            throw cancellation
        }

        // A request made while the finger was down took the reveal over.
        if (!revealDrag.isActive) return@awaitEachGesture

        val currentTravel = travel()
        if (isReleased && currentTravel != 0f) {
            // The lift itself counts: a finger that rested before lifting releases without speed.
            currentEvent.changes.fastFirstOrNull { it.changedToUpIgnoreConsumed() }
                ?.let(velocityTracker::addPointerInputChange)
            val velocity = velocityTracker.calculateVelocity(maximumVelocity).x / currentTravel
            val velocityThreshold = ReleaseVelocityThreshold.toPx() / abs(currentTravel)
            onRelease(releasesOpen(reveal.progress, velocity, velocityThreshold), velocity)
        } else {
            // The system cancelled the gesture.
            onRelease(reveal.settledIsOpen, 0f)
        }
    }
}

/** Where the drawer claimed a drag, and the physical x distance the finger moved past touch slop. */
private class ClaimedDrag(val change: PointerInputChange, val overSlop: Float)

/**
 * Follows [down] until it passes touch slop and returns where the drawer claims the drag, or
 * `null` when the finger lifts first, a descendant claims the drag, or the drawer leaves it alone.
 * [logicalSign] turns physical x into logical x, positive toward the trailing edge.
 */
private suspend fun AwaitPointerEventScope.awaitClaimedDrag(
    down: PointerInputChange,
    logicalSign: () -> Float,
    restsClosed: () -> Boolean,
    onMove: (PointerInputChange) -> Unit,
): ClaimedDrag? {
    val slop = viewConfiguration.touchSlop
    var translation = Offset.Zero
    while (true) {
        val change = awaitPointerEvent().changes.fastFirstOrNull { it.id == down.id }
        if (change == null || !change.pressed || change.isConsumed) return null

        onMove(change)
        translation += change.positionChange()
        if (abs(translation.x) > slop || abs(translation.y) > slop) {
            val logical = Offset(translation.x * logicalSign(), translation.y)
            if (!isHorizontalDrag(logical)) return null
            // Horizontal from here on, so the control under the finger must not also activate,
            // even when the drawer does not move.
            change.consume()
            if (!claimsDrag(logical, restsClosed())) return null
            // The finger moves the drawer from the slop onward, as a scroll view moves its content.
            return ClaimedDrag(change, overSlop = translation.x - sign(translation.x) * slop)
        }
    }
}

/** The window edges where the system takes drags for its own gestures. */
private class SystemGestureArea(
    private val insets: WindowInsets,
    private val density: Density,
    private val windowSize: () -> Pair<Int, Int>,
) {
    var coordinates: LayoutCoordinates? = null

    fun contains(position: Offset): Boolean {
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return false
        val inWindow = coordinates.localToWindow(position)
        val (width, height) = windowSize()
        return inWindow.x < insets.getLeft(density, LayoutDirection.Ltr) ||
            inWindow.x >= width - insets.getRight(density, LayoutDirection.Ltr) ||
            inWindow.y >= height - insets.getBottom(density)
    }
}

@Composable
private fun rememberSystemGestureArea(insets: WindowInsets): SystemGestureArea {
    val density = LocalDensity.current
    val rootView = LocalView.current.rootView
    return remember(insets, density, rootView) {
        SystemGestureArea(insets, density) { rootView.width to rootView.height }
    }
}

/**
 * Whether system animations are on. Compose animations already follow the system duration scale,
 * so with animations off the reveal settles immediately; this also keeps even a drag free of the
 * decorative content scale.
 */
@Composable
private fun rememberAnimationsEnabled(): Boolean {
    var isEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val motionDurationScale = coroutineContext[MotionDurationScale] ?: return@LaunchedEffect
        snapshotFlow { motionDurationScale.scaleFactor != 0f }.collect { isEnabled = it }
    }
    return isEnabled
}

/** Keeps touches from reaching the content beneath it without consuming them. */
@Composable
private fun InputBlocker(modifier: Modifier) {
    Box(modifier.pointerInput(Unit) {})
}

/**
 * The drawer content as one group: it grows from a slightly reduced scale and fades in with the
 * reveal, and its actions respond and reach assistive technology only once the drawer has settled
 * open. It is padded by [insets] once, so drawer content does not apply them again.
 */
@Composable
private fun DrawerContentGroup(
    reveal: DrawerReveal,
    phase: DrawerPhase,
    hasDecorativeScale: Boolean,
    isRtl: Boolean,
    insets: WindowInsets,
    acceptsFocus: Boolean,
    focusRequester: FocusRequester,
    onHasFocusChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .windowInsetsPadding(insets)
            .graphicsLayer {
                val progress = reveal.progress
                // A render transform after layout, so text never reflows as it grows.
                val scale = if (hasDecorativeScale) lerp(ClosedContentScale, 1f, progress) else 1f
                scaleX = scale
                scaleY = scale
                alpha = progress
                transformOrigin = TransformOrigin(if (isRtl) 1f else 0f, 0.5f)
            }
            .focusRequester(focusRequester)
            // Keeps focus from entering unless [acceptsFocus]; focus already inside is cleared by
            // DrawerFocusTransitions.
            .focusProperties {
                if (!acceptsFocus) onEnter = { cancelFocusChange() }
            }
            .onFocusChanged { onHasFocusChange(it.hasFocus) }
            .focusGroup()
            .then(if (phase == DrawerPhase.Open) Modifier else Modifier.clearAndSetSemantics {}),
    ) {
        content()
    }
}

/** Measures [content] but places it only while [isPlaced] holds, reading it at placement. */
@Composable
private fun PlacedWhile(isPlaced: () -> Boolean, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(
            width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth,
            height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight,
        ) {
            if (isPlaced()) placeables.forEach { it.place(0, 0) }
        }
    }
}

/**
 * The drawer's width in a window [maxWidth] wide: [DrawerWidthRatio] of it, bounded to a readable
 * [MaximumDrawerWidth] beside the [leadingInset] its content is padded by, and always leaving
 * [MinimumDismissalWidth] of the pushed screen visible before the [trailingInset], so the pushed
 * screen can still be tapped to dismiss the drawer.
 */
internal fun drawerWidth(maxWidth: Dp, leadingInset: Dp = 0.dp, trailingInset: Dp = 0.dp): Dp =
    minOf(
        maxWidth * DrawerWidthRatio,
        MaximumDrawerWidth + leadingInset,
        maxWidth - trailingInset - MinimumDismissalWidth,
    ).coerceAtLeast(0.dp)

internal const val DrawerWidthRatio = 0.85f
internal val MaximumDrawerWidth = 360.dp
internal val MinimumDismissalWidth = 48.dp
internal const val ClosedContentScale = 0.96f
internal const val LightScrimAlpha = 0.18f
internal const val DarkScrimAlpha = 0.24f
private val ForegroundCornerRadius = 24.dp
private val EdgeWidth = 1.dp

/** Per second. A release at least this fast settles in its direction wherever the drawer is. */
private val ReleaseVelocityThreshold = 400.dp

@Composable
private fun PushDrawerPreviewContent(reveal: DrawerReveal) {
    TycheTheme {
        Surface {
            PushDrawer(
                isOpen = reveal.targetIsOpen,
                onOpenChange = {},
                reveal = reveal,
                drawerContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(text = "Account", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Profile")
                        Text(text = "Sign out")
                    }
                },
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(count = 30) { index ->
                        Text(text = "Row $index", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PushDrawerClosedPreview() {
    PushDrawerPreviewContent(reveal = remember { DrawerReveal(isOpen = false) })
}

@PreviewLightDark
@Composable
private fun PushDrawerHalfOpenPreview() {
    PushDrawerPreviewContent(reveal = remember { DrawerReveal.heldAt(0.5f) })
}

@PreviewLightDark
@Composable
private fun PushDrawerOpenedPreview() {
    PushDrawerPreviewContent(reveal = remember { DrawerReveal(isOpen = true) })
}

@PreviewLightDark
@Composable
private fun PushDrawerOpenedRightToLeftPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PushDrawerPreviewContent(reveal = remember { DrawerReveal(isOpen = true) })
    }
}
