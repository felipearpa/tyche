package com.felipearpa.tyche.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlin.math.abs
import kotlin.math.sqrt

/** Where the drawer is in its reveal. */
internal enum class DrawerPhase {
    Closed,
    Opening,
    Open,
    Closing,
    Dragging;

    /**
     * Outside the closed endpoint the drawer owns interaction: the pushed screen is blocked and
     * hidden from accessibility, and only the dismissal surface responds on it.
     */
    val isModal: Boolean
        get() = this != Closed
}

/**
 * The drawer's reveal: one normalized progress, 0 closed and 1 open, that every visual property
 * derives from.
 *
 * A request animates the progress toward an endpoint from the value on screen, and a drag takes it
 * over from that same value, so reversing or grabbing a transition midway continues from where it
 * is. All members are used from the main thread.
 */
@Stable
internal class DrawerReveal(isOpen: Boolean) {
    /** The progress on screen, always within 0 and 1. */
    var progress by mutableFloatStateOf(endpointOf(isOpen))
        private set

    /** The endpoint the reveal is heading for, or rests at, while no drag drives it. */
    var targetIsOpen by mutableStateOf(isOpen)
        private set

    /**
     * The endpoint the reveal last came to rest at. A cancelled drag returns there, even when it
     * took over a transition that was heading for the other endpoint.
     */
    var settledIsOpen by mutableStateOf(isOpen)
        private set

    private var activeDrag by mutableStateOf<Drag?>(null)

    /** Progress per second of the running transition. */
    private var velocity = 0f
    private var transition: Job? = null

    val phase: DrawerPhase
        get() = when {
            activeDrag != null -> DrawerPhase.Dragging
            targetIsOpen -> if (progress >= 1f) DrawerPhase.Open else DrawerPhase.Opening
            else -> if (progress <= 0f) DrawerPhase.Closed else DrawerPhase.Closing
        }

    /**
     * Hands the reveal to a drag, starting from the progress on screen. A running transition stops
     * where it is, so grabbing it never jumps.
     */
    fun startDrag(): Drag {
        transition?.cancel()
        velocity = 0f
        return Drag(origin = progress).also { activeDrag = it }
    }

    /**
     * Animates toward an endpoint from the progress on screen, replacing any running transition
     * and ending any drag. [initialVelocity] is in progress per second; by default a transition
     * that reverses another keeps its momentum.
     */
    suspend fun animateTo(isOpen: Boolean, initialVelocity: Float = velocity) {
        transition?.cancel()
        activeDrag = null
        targetIsOpen = isOpen

        val target = endpointOf(isOpen)
        coroutineScope {
            transition = coroutineContext.job
            animate(
                initialValue = progress,
                targetValue = target,
                initialVelocity = velocityWithoutOvershoot(initialVelocity, from = progress, to = target),
                animationSpec = RevealSpec,
            ) { value, currentVelocity ->
                progress = value.coerceIn(0f, 1f)
                velocity = currentVelocity
            }
        }
        // Only reached when no other request, drag, or cancellation replaced this transition.
        velocity = 0f
        settledIsOpen = isOpen
    }

    /** A drag moving the reveal from [origin] by a translation measured in drawer widths. */
    inner class Drag internal constructor(val origin: Float) {
        /** Whether this drag still drives the reveal; a request made meanwhile takes it over. */
        val isActive: Boolean
            get() = activeDrag === this

        /** [translation] is logical: positive toward open. */
        fun translateTo(translation: Float) {
            if (isActive) {
                progress = (origin + translation).coerceIn(0f, 1f)
            }
        }
    }

    companion object {
        /** A reveal held mid-drag at [progress], as while a finger rests on the drawer. */
        fun heldAt(progress: Float): DrawerReveal =
            DrawerReveal(isOpen = true).apply { startDrag().translateTo(progress - 1f) }
    }
}

/**
 * Whether the drawer takes a drag once it passes touch slop. [translation] is logical: x points
 * toward the trailing edge. A drag must be predominantly horizontal, and a drawer resting closed
 * only opens.
 */
internal fun claimsDrag(translation: Offset, restsClosed: Boolean): Boolean =
    isHorizontalDrag(translation) && !(restsClosed && translation.x < 0f)

internal fun isHorizontalDrag(translation: Offset): Boolean = abs(translation.x) > abs(translation.y)

/**
 * The endpoint a released drag settles at: the direction of a fling at least [velocityThreshold]
 * fast, otherwise the nearer endpoint. Velocities are in progress per second, positive toward open.
 */
internal fun releasesOpen(progress: Float, velocity: Float, velocityThreshold: Float): Boolean =
    if (abs(velocity) >= velocityThreshold) velocity > 0f else progress >= 0.5f

/**
 * A critically damped spring overshoots its target when it starts toward it faster than its
 * natural frequency times the remaining distance, so faster starts are capped there. Motion away
 * from the target keeps its velocity; the spring turns it around.
 */
internal fun velocityWithoutOvershoot(velocity: Float, from: Float, to: Float): Float {
    val distance = to - from
    if (velocity * distance <= 0f) return velocity
    val limit = RevealNaturalFrequency * abs(distance)
    return velocity.coerceIn(-limit, limit)
}

private fun endpointOf(isOpen: Boolean) = if (isOpen) 1f else 0f

private const val RevealStiffness = 500f

/** Radians per second; a spring's natural frequency is the square root of its stiffness. */
private val RevealNaturalFrequency = sqrt(RevealStiffness)

/**
 * Critically damped, so the reveal settles without overshooting either endpoint. The threshold is
 * a thousandth of the drawer width, below a pixel on phones.
 */
private val RevealSpec: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = RevealStiffness,
    visibilityThreshold = 0.001f,
)
