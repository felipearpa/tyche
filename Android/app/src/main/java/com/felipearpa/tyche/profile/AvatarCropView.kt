package com.felipearpa.tyche.profile

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.R
import com.felipearpa.tyche.ui.theme.LocalBoxSpacing
import androidx.compose.foundation.Canvas as ComposeCanvas
import com.felipearpa.tyche.ui.R as SharedR
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pan + pinch-zoom crop over a fixed circular window on a near-black surface.
 * The image always covers the window; zoom is bounded by [AvatarCrop] and anchored at the
 * pinch centroid. A dual-scale preview strip below the window shows the crop at Profile
 * size and at row size.
 */
@Composable
fun AvatarCropView(
    bitmap: Bitmap,
    username: String,
    onConfirm: (androidx.compose.ui.geometry.Rect) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
    val view = LocalView.current

    var steadyScale by remember(bitmap) { mutableFloatStateOf(0f) }
    var steadyOffset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var isAtBoundary by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SURFACE_COLOR)
            // The screen is edge-to-edge; without this the action bar lands inside the system
            // gesture-navigation zone, which swallows taps.
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val density = LocalDensity.current
            val margin = with(density) { LocalBoxSpacing.current.huge.toPx() }
            // The height cap keeps the circle inside the gesture area above the preview strip
            // and action bar, matching the iOS sizing rule.
            val windowSide = min(
                constraints.maxWidth.toFloat() - margin,
                constraints.maxHeight.toFloat() * 0.5f,
            )
            val crop = AvatarCrop(imageSize = imageSize, windowSide = windowSide)
            val scale = if (steadyScale == 0f) crop.minScale else crop.clampScale(steadyScale)
            val offset = crop.clampOffset(steadyOffset, scale)

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .pointerInput(crop) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val currentScale =
                                    if (steadyScale == 0f) crop.minScale else steadyScale
                                val anchor = centroid - Offset(
                                    size.width / 2f,
                                    size.height / 2f,
                                )

                                val wantedScale = currentScale * zoom
                                val newScale = crop.clampScale(wantedScale)

                                val zoomedOffset = crop.offset(
                                    zoomingTo = newScale,
                                    from = currentScale,
                                    offset = steadyOffset,
                                    anchor = anchor,
                                )
                                val wantedOffset = zoomedOffset + pan
                                val newOffset = crop.clampOffset(wantedOffset, newScale)

                                val clamping =
                                    wantedScale != newScale || wantedOffset != newOffset
                                if (clamping && !isAtBoundary) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                isAtBoundary = clamping

                                steadyScale = newScale
                                steadyOffset = newOffset
                            }
                        },
                ) {
                    TransformedImage(
                        bitmap = bitmap,
                        imageSize = imageSize,
                        scale = scale,
                        offset = offset,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    ComposeCanvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                    ) {
                        drawRect(color = SCRIM_COLOR)
                        drawCircle(
                            color = Color.Transparent,
                            radius = windowSide / 2f,
                            blendMode = BlendMode.Clear,
                        )
                        drawCircle(
                            color = HAIRLINE_COLOR,
                            radius = windowSide / 2f,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }

                }

                PreviewStrip(
                    bitmap = bitmap,
                    imageSize = imageSize,
                    scale = scale,
                    offset = offset,
                    windowSide = windowSide,
                    username = username,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LocalBoxSpacing.current.extraLarge),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LocalBoxSpacing.current.large,
                            vertical = LocalBoxSpacing.current.large,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = stringResource(id = SharedR.string.cancel_action),
                            color = Color.White,
                        )
                    }

                    Button(onClick = { onConfirm(crop.cropRect(scale = scale, offset = offset)) }) {
                        Text(text = stringResource(id = R.string.use_photo_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformedImage(
    bitmap: Bitmap,
    imageSize: Size,
    scale: Float,
    offset: Offset,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .requiredSize(
                width = with(density) { (imageSize.width * scale).toDp() },
                height = with(density) { (imageSize.height * scale).toDp() },
            ),
    )
}

@Composable
private fun PreviewStrip(
    bitmap: Bitmap,
    imageSize: Size,
    scale: Float,
    offset: Offset,
    windowSide: Float,
    username: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            LocalBoxSpacing.current.extraLarge,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CropPreview(
            bitmap = bitmap,
            imageSize = imageSize,
            scale = scale,
            offset = offset,
            windowSide = windowSide,
            diameter = PROFILE_PREVIEW_SIZE.dp,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalBoxSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CropPreview(
                bitmap = bitmap,
                imageSize = imageSize,
                scale = scale,
                offset = offset,
                windowSide = windowSide,
                diameter = ROW_PREVIEW_SIZE.dp,
            )

            Text(
                text = username,
                style = MaterialTheme.typography.bodyMedium,
                color = PREVIEW_LABEL_COLOR,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CropPreview(
    bitmap: Bitmap,
    imageSize: Size,
    scale: Float,
    offset: Offset,
    windowSide: Float,
    diameter: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val previewFactor = with(density) { diameter.toPx() } / windowSide

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape),
    ) {
        TransformedImage(
            bitmap = bitmap,
            imageSize = imageSize,
            scale = scale * previewFactor,
            offset = offset * previewFactor,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

private const val PROFILE_PREVIEW_SIZE = 96
private const val ROW_PREVIEW_SIZE = 32

private val SURFACE_COLOR = Color(0xFF0A0A0A)
private val SCRIM_COLOR = Color.Black.copy(alpha = 0.62f)
private val HAIRLINE_COLOR = Color.White.copy(alpha = 0.24f)
private val PREVIEW_LABEL_COLOR = Color.White.copy(alpha = 0.85f)
