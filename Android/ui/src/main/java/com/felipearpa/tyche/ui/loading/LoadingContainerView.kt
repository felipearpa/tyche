package com.felipearpa.tyche.ui.loading

import android.os.Build
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import com.felipearpa.tyche.ui.theme.TycheTheme

/**
 * Full-screen loading host for a blocking operation.
 *
 * The host always composes [content] in the same place, so toggling [isLoading] keeps the screen's
 * state: its text, selection, step, and scroll position. While loading:
 * - on Android 12 (API 31) and later the content is softly blurred; below API 31 it stays sharp.
 *   No veil, scrim, tint, or card is drawn over it;
 * - a clear barrier takes every touch, and focus is taken out of the content and cannot enter it,
 *   so it cannot be tapped, scrolled, typed into, or activated. A focused field loses focus and
 *   the software keyboard closes;
 * - the content is hidden from accessibility services, which find a single localized loading
 *   status instead;
 * - the Tyche ball is centered in the host's safe area, which excludes system bars, cutouts,
 *   caption bars, and a visible keyboard.
 *
 * When loading ends, all of the above is removed and the content is interactive again. System Back
 * is not intercepted; the flow decides what it does.
 *
 * Place the host around the whole screen, outside form padding, so it fills the area the screen is
 * given. The host paints no background of its own: the screen keeps the background its container
 * gives it, and a host without content shows the ball on that background.
 */
@Composable
fun LoadingContainerView(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var contentHasFocus by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isLoading) {
        if (isLoading && contentHasFocus) focusManager.clearFocus(force = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .loadingBlur(isLoading = isLoading)
                .focusProperties {
                    if (isLoading) onEnter = { cancelFocusChange() }
                }
                .onFocusChanged { contentHasFocus = it.hasFocus }
                .focusGroup()
                .then(if (isLoading) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            content()
        }

        if (isLoading) {
            LoadingIndicatorLayer()
        }
    }
}

/** A host with no screen behind it: the ball on the background its container gives it. */
@Composable
fun LoadingContainerView(isLoading: Boolean, modifier: Modifier = Modifier) {
    LoadingContainerView(isLoading = isLoading, modifier = modifier) {}
}

/** Blur is applied only where Compose renders it (API 31+); older versions keep the content sharp. */
private fun Modifier.loadingBlur(isLoading: Boolean): Modifier =
    if (isLoading && isBlurSupported) {
        blur(radius = LoadingBlurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        this
    }

private val isBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The barrier that keeps touches away from the covered content, and the ball with its status. */
@Composable
private fun LoadingIndicatorLayer() {
    val status = stringResource(id = R.string.loading_status)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(pass = PointerEventPass.Initial)
                            .changes
                            .forEach(PointerInputChange::consume)
                    }
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Its own node, so the ball's image semantics are replaced rather than merged into it.
        Box(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = status
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            BallSpinner()
        }
    }
}

/**
 * Shared by every loading screen. Calibrated to look like iOS's 6-point blur: Android's blur turns
 * a radius into a Gaussian sigma of about 0.58 × radius, while SwiftUI's radius acts as the sigma.
 */
private val LoadingBlurRadius: Dp = 10.dp

@Composable
private fun LoadingPreviewForm() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = LocalBoxSpacing.current.medium),
        verticalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
    ) {
        TextField(
            value = "Gambler",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Continue")
        }
    }
}

@PreviewLightDark
@Composable
private fun LoadingContainerViewContentPreview() {
    TycheTheme {
        Surface {
            LoadingContainerView(isLoading = true) {
                LoadingPreviewForm()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LoadingContainerViewNoContentPreview() {
    TycheTheme {
        Surface {
            LoadingContainerView(isLoading = true)
        }
    }
}
