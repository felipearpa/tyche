package com.felipearpa.tyche.ui

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeMonotonicallyIncreasing
import io.kotest.matchers.collections.shouldBeMonotonicallyDecreasing
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class DrawerRevealTest {
    @Test
    fun `given a closed reveal when it animates open then progress rises to the open endpoint and settles there`() =
        runTest {
            val driver = RevealDriver(DrawerReveal(isOpen = false), this)

            driver.request(isOpen = true)
            val trajectory = driver.frames(count = 60)

            trajectory.shouldBeMonotonicallyIncreasing()
            trajectory.last() shouldBe 1f
            driver.reveal.phase shouldBe DrawerPhase.Open
            driver.reveal.settledIsOpen.shouldBeTrue()
        }

    @Test
    fun `given an opening transition when a close request arrives then progress reverses from its displayed value without reaching open`() =
        runTest {
            val driver = RevealDriver(DrawerReveal(isOpen = false), this)
            driver.request(isOpen = true)
            val displayed = driver.frames(count = 6).last()

            driver.request(isOpen = false)
            val afterReversal = driver.frames(count = 60)

            displayed shouldBeGreaterThan 0.1f
            displayed shouldBeLessThan 0.9f
            // Momentum may carry it a little further before the spring turns it around.
            afterReversal.first() shouldBe (displayed plusOrMinus 0.1f)
            afterReversal.max() shouldBeLessThan 1f
            afterReversal.last() shouldBe 0f
            driver.reveal.phase shouldBe DrawerPhase.Closed
            driver.reveal.settledIsOpen.shouldBeFalse()
        }

    @Test
    fun `given an opening transition when a drag takes over then progress holds the displayed value and the transition stops`() =
        runTest {
            val driver = RevealDriver(DrawerReveal(isOpen = false), this)
            val transition = driver.request(isOpen = true)
            val displayed = driver.frames(count = 5).last()

            val drag = driver.reveal.startDrag()
            val held = driver.frames(count = 10)
            drag.translateTo(-0.1f)

            held.forEach { it shouldBe displayed }
            transition.isCancelled.shouldBeTrue()
            driver.reveal.phase shouldBe DrawerPhase.Dragging
            driver.reveal.progress shouldBe (displayed - 0.1f plusOrMinus 0.0001f)
            // The opening never came to rest, so a cancelled drag would return closed.
            driver.reveal.settledIsOpen.shouldBeFalse()
        }

    @Test
    fun `given a drag when a request arrives then the request takes the reveal over and the drag stops moving it`() =
        runTest {
            val driver = RevealDriver(DrawerReveal(isOpen = true), this)
            val drag = driver.reveal.startDrag()
            drag.translateTo(-0.4f)

            driver.request(isOpen = true)
            driver.frames(count = 1)
            drag.translateTo(-0.9f)
            driver.frames(count = 60)

            drag.isActive.shouldBeFalse()
            driver.reveal.progress shouldBe 1f
            driver.reveal.phase shouldBe DrawerPhase.Open
        }

    @Test
    fun `given a fast release toward the endpoint when it settles then progress never passes the endpoint`() =
        runTest {
            val driver = RevealDriver(DrawerReveal(isOpen = false), this)
            driver.reveal.startDrag().translateTo(0.8f)

            driver.request(isOpen = true, velocity = 50f)
            val trajectory = driver.frames(count = 60)

            trajectory.shouldBeMonotonicallyIncreasing()
            trajectory.last() shouldBe 1f
            driver.reveal.settledIsOpen.shouldBeTrue()
        }

    @Test
    fun `given a closing transition when it settles then progress falls steadily to the closed endpoint`() =
        runTest {
            val driver = RevealDriver(DrawerReveal(isOpen = true), this)

            driver.request(isOpen = false)
            val trajectory = driver.frames(count = 60)

            trajectory.shouldBeMonotonicallyDecreasing()
            trajectory.last() shouldBe 0f
            driver.reveal.phase shouldBe DrawerPhase.Closed
        }

    @Test
    fun `given a drag beyond an endpoint then progress stays within the endpoints`() {
        val reveal = DrawerReveal(isOpen = true)
        val drag = reveal.startDrag()

        drag.translateTo(0.5f)
        val beyondOpen = reveal.progress
        drag.translateTo(-1.5f)
        val beyondClosed = reveal.progress

        beyondOpen shouldBe 1f
        beyondClosed shouldBe 0f
    }

    @Test
    fun `given a reveal held at a progress then it is dragging at that progress`() {
        val reveal = DrawerReveal.heldAt(0.5f)

        reveal.progress shouldBe 0.5f
        reveal.phase shouldBe DrawerPhase.Dragging
        reveal.phase.isModal.shouldBeTrue()
    }

    @Test
    fun `given a closed drawer then only the closed phase is not modal`() {
        DrawerPhase.entries.filter { !it.isModal } shouldBe listOf(DrawerPhase.Closed)
    }

    @Test
    fun `given a drawer resting closed when a drag heads toward the trailing edge then it is claimed`() {
        claimsDrag(Offset(x = 12f, y = 3f), restsClosed = true).shouldBeTrue()
    }

    @Test
    fun `given a drawer resting closed when a drag heads toward the leading edge then it is not claimed`() {
        claimsDrag(Offset(x = -12f, y = 3f), restsClosed = true).shouldBeFalse()
    }

    @Test
    fun `given a drawer that is not resting closed when a drag heads toward the leading edge then it is claimed`() {
        claimsDrag(Offset(x = -12f, y = 3f), restsClosed = false).shouldBeTrue()
    }

    @Test
    fun `given a drag that is not predominantly horizontal then it is not claimed`() {
        claimsDrag(Offset(x = 9f, y = 12f), restsClosed = false).shouldBeFalse()
        claimsDrag(Offset(x = 10f, y = -10f), restsClosed = false).shouldBeFalse()
    }

    @Test
    fun `given a slow release then the nearer endpoint wins`() {
        releasesOpen(progress = 0.6f, velocity = -1f, velocityThreshold = 1.2f).shouldBeTrue()
        releasesOpen(progress = 0.4f, velocity = 1f, velocityThreshold = 1.2f).shouldBeFalse()
    }

    @Test
    fun `given a fast release then its direction wins wherever the drawer is`() {
        releasesOpen(progress = 0.1f, velocity = 1.2f, velocityThreshold = 1.2f).shouldBeTrue()
        releasesOpen(progress = 0.9f, velocity = -3f, velocityThreshold = 1.2f).shouldBeFalse()
    }

    @Test
    fun `given a start toward the target faster than the spring absorbs then the velocity is capped`() {
        val limit = sqrt(500f) * 0.2f

        velocityWithoutOvershoot(velocity = 50f, from = 0.8f, to = 1f) shouldBe (limit plusOrMinus 0.0001f)
        velocityWithoutOvershoot(velocity = -50f, from = 0.2f, to = 0f) shouldBe (-limit plusOrMinus 0.0001f)
    }

    @Test
    fun `given a start away from the target or within the limit then the velocity is kept`() {
        velocityWithoutOvershoot(velocity = -3f, from = 0.6f, to = 1f) shouldBe -3f
        velocityWithoutOvershoot(velocity = 1f, from = 0.5f, to = 1f) shouldBe 1f
    }
}

/** Drives a reveal frame by frame with a manual clock, as Compose's frame clock would. */
private class RevealDriver(val reveal: DrawerReveal, private val scope: TestScope) {
    private val clock = BroadcastFrameClock()
    private var frameTimeNanos = 0L

    fun request(isOpen: Boolean, velocity: Float? = null): Job =
        (scope as CoroutineScope).launch(clock) {
            if (velocity == null) reveal.animateTo(isOpen) else reveal.animateTo(isOpen, velocity)
        }.also { scope.testScheduler.runCurrent() }

    /** Sends [count] frames 16 ms apart and returns the progress after each. */
    fun frames(count: Int): List<Float> = List(count) {
        frameTimeNanos += FrameIntervalNanos
        clock.sendFrame(frameTimeNanos)
        scope.testScheduler.runCurrent()
        reveal.progress
    }

    private companion object {
        const val FrameIntervalNanos = 16_666_667L
    }
}
