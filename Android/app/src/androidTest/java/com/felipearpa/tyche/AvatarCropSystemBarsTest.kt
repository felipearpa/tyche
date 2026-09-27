package com.felipearpa.tyche

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.toSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.felipearpa.tyche.profile.AvatarCropView
import com.felipearpa.tyche.ui.theme.TycheTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.felipearpa.tyche.ui.R as SharedR

/**
 * The crop screen's surface is dark in both themes. While it shows, the system bar icons are
 * light, and the activity's own icon appearance returns once it leaves. Its actions stay inside
 * the area clear of the system bars and a display cutout, including in landscape.
 */
class AvatarCropSystemBarsTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theCropScreenShowsLightSystemBarIconsAndRestoresTheActivityIconsWhenItLeaves() {
        makeEdgeToEdge()
        setAppearance(lightStatusBars = true, lightNavigationBars = true)
        val bitmap = sampleBitmap()
        var isCropping by mutableStateOf(true)
        composeTestRule.setContent {
            TycheTheme {
                if (isCropping) {
                    AvatarCropView(bitmap = bitmap, username = "gambler", onConfirm = {}, onCancel = {})
                }
            }
        }
        composeTestRule.waitForIdle()

        assertAppearance(lightStatusBars = false, lightNavigationBars = false)

        isCropping = false
        composeTestRule.waitForIdle()

        assertAppearance(lightStatusBars = true, lightNavigationBars = true)
    }

    @Test
    fun theCropActionsStayInsideTheSafeAreaInLandscape() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        composeTestRule.waitUntil(timeoutMillis = ROTATION_TIMEOUT_MILLIS) {
            composeTestRule.runOnUiThread {
                composeTestRule.activity.resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
            }
        }
        makeEdgeToEdge()
        val bitmap = sampleBitmap()
        composeTestRule.setContent {
            TycheTheme {
                AvatarCropView(bitmap = bitmap, username = "gambler", onConfirm = {}, onCancel = {})
            }
        }
        composeTestRule.waitForIdle()

        assertInsideSafeArea(action(composeTestRule.activity.getString(SharedR.string.cancel_action)))
        assertInsideSafeArea(action(composeTestRule.activity.getString(R.string.use_photo_action)))
    }

    /**
     * As MainActivity is configured before its window is attached; the cutout mode set on an
     * attached window must be reapplied to reach the window manager.
     */
    private fun makeEdgeToEdge() {
        composeTestRule.runOnUiThread {
            val activity = composeTestRule.activity
            activity.enableEdgeToEdge()
            activity.window.attributes = activity.window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    private fun setAppearance(lightStatusBars: Boolean, lightNavigationBars: Boolean) {
        composeTestRule.runOnUiThread {
            val window = composeTestRule.activity.window
            WindowCompat.getInsetsController(window, window.decorView).run {
                isAppearanceLightStatusBars = lightStatusBars
                isAppearanceLightNavigationBars = lightNavigationBars
            }
        }
    }

    private fun assertAppearance(lightStatusBars: Boolean, lightNavigationBars: Boolean) {
        composeTestRule.runOnUiThread {
            val window = composeTestRule.activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertEquals("Light status bar icons", lightStatusBars, controller.isAppearanceLightStatusBars)
            assertEquals(
                "Light navigation bar icons",
                lightNavigationBars,
                controller.isAppearanceLightNavigationBars,
            )
        }
    }

    private fun action(text: String) = composeTestRule.onNode(hasText(text) and hasClickAction())

    /** Clear of the system bars and the display cutout on every side, fully shown. */
    private fun assertInsideSafeArea(node: SemanticsNodeInteraction) {
        val semanticsNode = node.assertIsDisplayed().fetchSemanticsNode()
        val bounds = Rect(offset = semanticsNode.positionInWindow, size = semanticsNode.size.toSize())
        val safeArea = composeTestRule.runOnUiThread {
            val decorView = composeTestRule.activity.window.decorView
            val insets = requireNotNull(ViewCompat.getRootWindowInsets(decorView)).getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            Rect(
                left = insets.left.toFloat(),
                top = insets.top.toFloat(),
                right = (decorView.width - insets.right).toFloat(),
                bottom = (decorView.height - insets.bottom).toFloat(),
            )
        }
        assertTrue(
            "The action $bounds must lie inside the safe area $safeArea",
            bounds.left >= safeArea.left - PIXEL_TOLERANCE &&
                bounds.top >= safeArea.top - PIXEL_TOLERANCE &&
                bounds.right <= safeArea.right + PIXEL_TOLERANCE &&
                bounds.bottom <= safeArea.bottom + PIXEL_TOLERANCE,
        )
    }

    private fun sampleBitmap(): Bitmap = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)

    private companion object {
        const val PIXEL_TOLERANCE = 1.5f
        const val ROTATION_TIMEOUT_MILLIS = 10_000L
    }
}
