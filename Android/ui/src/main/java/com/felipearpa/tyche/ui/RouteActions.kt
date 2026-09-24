package com.felipearpa.tyche.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Runs [action], a user action that leaves a navigation route, only while this lifecycle owner is at
 * least started. Call it on the route's back stack entry, which is also the [LocalLifecycleOwner] of
 * the route's content.
 *
 * Leaving a route moves its entry below started at once, while its screen stays composed, and keeps
 * taking input, until it next recomposes and then through its exit transition. A second activation
 * in that window, such as a double tap or two Enter keys handled in one frame, therefore does nothing
 * instead of opening the destination again. Started rather than resumed, which `dropUnlessResumed`
 * requires, so a route that is still fading in after navigation or Back keeps responding.
 */
fun LifecycleOwner.runIfStarted(action: () -> Unit) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) action()
}
