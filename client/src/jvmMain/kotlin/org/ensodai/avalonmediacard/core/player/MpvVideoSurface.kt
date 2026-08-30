package org.ensodai.avalonmediacard.core.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

@Composable
fun MpvVideoSurface(
    controller: DesktopMpvPlaybackController,
    modifier: Modifier = Modifier
) {
    val frameCountState = controller.frameCount.collectAsState()

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentFrame = frameCountState.value
        val pool = controller.bufferPool

        if (pool != null && !pool.isReleased && currentFrame > 0L) {
            val frontIndex = pool.swapBeforeRead()
            val skiaBitmap = pool.bitmaps[frontIndex]

            val skiaImage = try {
                Image.makeFromBitmap(skiaBitmap)
            } catch (_: Throwable) {
                return@Canvas
            }

            val canvasWidth = size.width
            val canvasHeight = size.height
            val videoWidth = pool.width.toFloat()
            val videoHeight = pool.height.toFloat()

            if (videoWidth > 0f && videoHeight > 0f && canvasWidth > 0f && canvasHeight > 0f) {
                val scale = minOf(canvasWidth / videoWidth, canvasHeight / videoHeight)
                val dstWidth = videoWidth * scale
                val dstHeight = videoHeight * scale
                val offsetX = (canvasWidth - dstWidth) / 2f
                val offsetY = (canvasHeight - dstHeight) / 2f

                drawIntoCanvas { canvas ->
                    canvas.skiaCanvas.drawImageRect(
                        image = skiaImage,
                        src = Rect.makeWH(videoWidth, videoHeight),
                        dst = Rect.makeXYWH(offsetX, offsetY, dstWidth, dstHeight),
                        samplingMode = SamplingMode.LINEAR,
                        paint = null,
                        strict = true
                    )
                }
            }
            skiaImage.close()
        }
    }
}
