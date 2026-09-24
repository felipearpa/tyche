package com.felipearpa.tyche.ui

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlin.math.roundToInt

/**
 * A drawer host with controls of every kind the pool list and pool home use, a synthetic horizontal
 * control that handles its own drags, and markers.
 */
internal class DrawerHarness(initiallyOpen: Boolean = false) {
    var isOpen by mutableStateOf(initiallyOpen)

    /** Whether the host follows the endpoints the drawer reports. */
    var acceptsChanges = true
    val reportedChanges = mutableListOf<Boolean>()
    val reveal = DrawerReveal(initiallyOpen)

    var hostRowClicks = 0
    var drawerRowClicks = 0
    var signOutClicks = 0
    var openerClicks = 0

    /** What the drawer row does besides counting, such as closing the drawer and navigating. */
    var onDrawerRowClick: () -> Unit = {}

    /** What the host row does besides counting, such as navigating. */
    var onHostRowClick: () -> Unit = {}
    var itemClicks = 0
    var selectedTab by mutableIntStateOf(0)
    val listState = LazyListState()
    val chipsState = LazyListState()

    /** Physical x distance each synthetic horizontal control has dragged itself in total. */
    var hostControlDrag = 0f
    var drawerControlDrag = 0f

    /** Line counts of every text layout of the wrapping drawer label. */
    val drawerLabelLayouts = mutableListOf<Int>()
}

internal object HarnessTags {
    const val Opener = "opener"
    const val HostRow = "hostRow"
    const val Tabs = "tabs"
    const val Chips = "chips"
    const val List = "list"
    const val DrawerRow = "drawerRow"
    const val HostControl = "hostControl"
    const val DrawerControl = "drawerControl"
}

/** The menu's first element. */
internal const val HarnessDrawerLabel =
    "A drawer label that is long enough to wrap onto a second line in the menu"

/** How [focusedNodeLabels] names the opener, the host row, and the menu's two actions. */
internal const val OpenerLabel = "${HarnessTags.Opener} Open menu"
internal const val HostRowLabel = "${HarnessTags.HostRow} Host row"
internal const val ProfileRowLabel = "${HarnessTags.DrawerRow} Profile"
internal const val SignOutRowLabel = "Sign out"
internal val DrawerActionLabels = listOf(ProfileRowLabel, SignOutRowLabel)

internal val HostBackground = Color(0xFFB2DFDB)
internal val MarkerSize = 48.dp
internal val BarWidth = 120.dp

/**
 * [menuTopPadding] moves the menu's actions down, as the app's account header does, so a control of
 * the pushed screen lies nearer the window's top than the menu's first action.
 */
@Composable
internal fun DrawerHarnessContent(
    harness: DrawerHarness,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    darkTheme: Boolean = false,
    systemGestureInsets: WindowInsets = WindowInsets(0.dp),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    menuTopPadding: Dp = 16.dp,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalLifecycleOwner provides lifecycleOwner,
    ) {
        TycheTheme(darkTheme = darkTheme) {
            // Black behind everything, so any transparency in the drawer surface would show.
            Box(modifier.fillMaxSize().background(Color.Black)) {
                PushDrawer(
                    isOpen = harness.isOpen,
                    onOpenChange = {
                        harness.reportedChanges += it
                        if (harness.acceptsChanges) harness.isOpen = it
                    },
                    drawerContent = { HarnessMenu(harness, topPadding = menuTopPadding) },
                    reveal = harness.reveal,
                    systemGestureInsets = systemGestureInsets,
                ) {
                    HarnessScreen(harness)
                }
            }
        }
    }
}

@Composable
private fun HarnessMenu(harness: DrawerHarness, topPadding: Dp) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(top = topPadding)) {
            Text(
                text = HarnessDrawerLabel,
                modifier = Modifier.padding(horizontal = 16.dp),
                onTextLayout = { harness.drawerLabelLayouts += it.lineCount },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .testTag(HarnessTags.DrawerRow)
                    .clickable {
                        harness.drawerRowClicks++
                        harness.onDrawerRowClick()
                    }
                    .padding(16.dp),
            ) {
                Text("Profile")
            }
            Row(Modifier.fillMaxWidth().clickable { harness.signOutClicks++ }.padding(16.dp)) {
                Text("Sign out")
            }
            HorizontalDragControl(
                onDrag = { harness.drawerControlDrag += it },
                modifier = Modifier.testTag(HarnessTags.DrawerControl),
            )
        }
        // Vertically centered on the content group, where its scale is anchored, so scaling only
        // changes its width.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(width = BarWidth, height = 48.dp)
                .background(Color.Blue),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HarnessScreen(harness: DrawerHarness) {
    Column(Modifier.fillMaxSize().background(HostBackground)) {
        Row {
            Box(Modifier.size(MarkerSize).background(Color.Red))
            Box(
                Modifier
                    .size(48.dp)
                    .testTag(HarnessTags.Opener)
                    .semantics { contentDescription = "Open menu" }
                    .clickable(role = Role.Button) {
                        harness.openerClicks++
                        harness.isOpen = !harness.isOpen
                    },
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .testTag(HarnessTags.HostRow)
                .clickable {
                    harness.hostRowClicks++
                    harness.onHostRowClick()
                },
        ) {
            Text("Host row", Modifier.align(Alignment.Center))
        }
        PrimaryTabRow(
            selectedTabIndex = harness.selectedTab,
            modifier = Modifier.testTag(HarnessTags.Tabs),
        ) {
            listOf("Scores", "Bets", "History").forEachIndexed { index, title ->
                Tab(
                    selected = harness.selectedTab == index,
                    onClick = { harness.selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        LazyRow(
            state = harness.chipsState,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag(HarnessTags.Chips),
        ) {
            items(count = 30) { index ->
                Text("Chip $index", Modifier.width(96.dp).padding(16.dp))
            }
        }
        HorizontalDragControl(
            onDrag = { harness.hostControlDrag += it },
            modifier = Modifier.testTag(HarnessTags.HostControl),
        )
        LazyColumn(
            state = harness.listState,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(HarnessTags.List),
        ) {
            items(count = 60) { index ->
                Text(
                    text = "Item $index",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { harness.itemClicks++ }
                        .padding(20.dp),
                )
            }
        }
    }
}

/**
 * A synthetic control that handles its own horizontal drags, as a slider does, without scrolling.
 * Neither drawer host has one; it stands for any such control added later.
 */
@Composable
private fun HorizontalDragControl(onDrag: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .draggable(state = rememberDraggableState(onDrag), orientation = Orientation.Horizontal),
    )
}

/**
 * Stands for the host route's lifecycle, which follows the activity's: an activity pause, such as
 * a share sheet's, moves it below resumed while the route stays the current one. Used on the main
 * thread only.
 */
internal class HarnessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }

    override val lifecycle: Lifecycle
        get() = registry

    fun moveTo(state: Lifecycle.State) {
        registry.currentState = state
    }
}

/** Sends the key through the system input pipeline, like a hardware keyboard, which leaves touch mode. */
internal fun ComposeTestRule.pressKey(keyCode: Int) {
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
    waitForIdle()
}

/**
 * Taps the center of the node tagged [tag] twice through the system input pipeline, like a finger,
 * without advancing the test clock, so no frame, and no recomposition, comes between the taps. With
 * the clock paused, both taps reach the app before the drawer or its host sees the first one's
 * effect.
 */
internal fun ComposeTestRule.tapTwiceWithinOneFrame(tag: String) {
    val node = onNodeWithTag(tag).fetchSemanticsNode()
    val center = node.positionOnScreen + Offset(node.size.width / 2f, node.size.height / 2f)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    repeat(2) {
        val downTime = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, center.x, center.y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
    }
    waitForIdle()
}

/**
 * Presses Tab as a hardware keyboard would, which leaves touch mode (clickable controls take focus
 * only outside it), then moves focus to the opener.
 */
internal fun ComposeTestRule.focusOpenerWithKeyboard() {
    pressKey(KeyEvent.KEYCODE_TAB)
    onNodeWithTag(HarnessTags.Opener).requestFocus()
    waitForIdle()
    onNodeWithTag(HarnessTags.Opener).assertIsFocused()
}

/**
 * Presses Tab until keyboard focus is on one of the menu's actions, at most three times, and
 * returns the label of the focused node. The first Tab may only leave touch mode.
 */
internal fun ComposeTestRule.tabIntoDrawer(): String {
    repeat(3) {
        pressKey(KeyEvent.KEYCODE_TAB)
        if (focusedNodeLabel() in DrawerActionLabels) return focusedNodeLabel()
    }
    return focusedNodeLabel()
}

/** The one focused node that assistive technology can reach, or "nothing". */
internal fun ComposeTestRule.focusedNodeLabel(): String = focusedNodeLabels().singleOrNull() ?: "nothing"

/**
 * Every focused node. The unmerged tree also holds nodes that are hidden from accessibility or not
 * placed, such as the actions of a closed drawer, so it shows focus that no one can see.
 */
internal fun ComposeTestRule.focusedNodeLabels(useUnmergedTree: Boolean = false): List<String> =
    onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true), useUnmergedTree)
        .fetchSemanticsNodes()
        .map { it.label() }

/** Its tag, description, and text, then those of its children, which the merged tree has merged in. */
private fun SemanticsNode.label(): String {
    val own = listOfNotNull(
        config.getOrElseNullable(SemanticsProperties.TestTag) { null },
        config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.joinToString(),
        config.getOrElseNullable(SemanticsProperties.Text) { null }?.joinToString(),
    )
    return (own + children.map { it.label() }).filter { it.isNotEmpty() }.joinToString(" ")
}

internal fun ComposeTestRule.rootWidth(): Float =
    onRoot().fetchSemanticsNode().size.width.toFloat()

internal fun ComposeTestRule.drawerWidthPx(): Float =
    with(density) { drawerWidth(rootWidth().toDp()).toPx() }

/**
 * Captures the root. A software-rendered emulator can miss the one-second pixel copy deadline, so
 * a failed copy is retried after a pause; a paused test clock keeps the same frame on screen.
 */
internal fun ComposeTestRule.captureRoot(): ImageBitmap {
    repeat(CaptureAttempts - 1) {
        try {
            return onRoot().captureToImage()
        } catch (error: AssertionError) {
            Log.w("DrawerHarness", "Retrying a root capture", error)
        } catch (error: RuntimeException) {
            Log.w("DrawerHarness", "Retrying a root capture", error)
        } catch (error: ComposeTimeoutException) {
            // Thrown while forcing the redraw before the copy; it extends Throwable, not Exception.
            Log.w("DrawerHarness", "Retrying a root capture", error)
        }
        Thread.sleep(CaptureRetryPauseMillis)
    }
    return onRoot().captureToImage()
}

private const val CaptureAttempts = 5
private const val CaptureRetryPauseMillis = 500L

internal fun ComposeTestRule.dpToPx(dp: Float): Float = with(density) { dp.dp.toPx() }

/** Advances the paused clock by [count] frames, reading [read] after each one. */
internal fun <T> ComposeTestRule.frames(count: Int, read: () -> T): List<T> =
    List(count) {
        mainClock.advanceTimeByFrame()
        waitForIdle()
        read()
    }

/** The first column in [row] whose pixel matches [predicate], scanning from [fromX]. */
internal fun PixelMap.firstX(row: Int, fromX: Int = 0, predicate: (Color) -> Boolean): Int? =
    (fromX until width).firstOrNull { predicate(this[it, row]) }

internal fun PixelMap.lastX(row: Int, untilX: Int = width, predicate: (Color) -> Boolean): Int? =
    (0 until untilX).lastOrNull { predicate(this[it, row]) }

internal fun Color.isMarkerRed(): Boolean = red > 0.6f && green < 0.3f && blue < 0.3f

/** The bar is pure blue faded over the white drawer surface: blue stays 1 and red drops. */
internal fun Color.isBarBlue(): Boolean = blue > 0.9f && blue - red > 0.1f

internal fun Offset.roundedX(): Int = x.roundToInt()

/**
 * A node of the accessibility tree that services such as TalkBack receive. [label] is its own
 * text or description; [subtreeLabel] adds its descendants', which a service reads for a control
 * that merges them, such as a row.
 */
internal data class AccessibleNode(
    val label: String,
    val subtreeLabel: String,
    val isClickable: Boolean,
    val paneTitle: String?,
)

internal fun List<AccessibleNode>.labels(): List<String> = map { it.label }.filter { it.isNotEmpty() }

internal fun List<AccessibleNode>.paneTitles(): List<String> = mapNotNull { it.paneTitle }

internal fun List<AccessibleNode>.hasClickable(label: String): Boolean =
    any { it.isClickable && label in it.subtreeLabel }

/**
 * Every node of the active window's accessibility tree, read the way an accessibility service
 * reads it, so it holds exactly what TalkBack can reach.
 */
internal fun reachableAccessibilityNodes(): List<AccessibleNode> {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    // Drops nodes cached before the last change, which would otherwise be read back stale.
    automation.clearCache()
    // The first read after the service connects can come before it sees the window.
    val root = (1..50).firstNotNullOfOrNull {
        automation.rootInActiveWindow ?: null.also { Thread.sleep(100) }
    }
    return buildList { addSubtree(checkNotNull(root) { "No active window to read" }) }
}

/** Adds [node] and its descendants, and returns the labels of all of them. */
private fun MutableList<AccessibleNode>.addSubtree(node: AccessibilityNodeInfo): String {
    val label = listOfNotNull(node.contentDescription, node.text).joinToString(" ")
    val index = size
    add(AccessibleNode(label, label, node.isClickable, node.paneTitle?.toString()))
    val descendantLabels = (0 until node.childCount).mapNotNull { node.getChild(it)?.let(::addSubtree) }
    val subtreeLabel = (listOf(label) + descendantLabels).filter { it.isNotEmpty() }.joinToString(" ")
    this[index] = this[index].copy(subtreeLabel = subtreeLabel)
    return subtreeLabel
}
