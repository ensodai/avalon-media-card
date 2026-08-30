package org.ensodai.avalonmediacard.core.player

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.ensodai.avalonmediacard.core.PlaybackController

@Composable
fun PlayerLifecycleHandler(
    controller: PlaybackController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner, controller) {
        var wasPlayingBeforeBackground = false

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    val isPip = (context as? Activity)?.isInPictureInPictureMode == true
                    if (!isPip && controller.state.isPlaying) {
                        wasPlayingBeforeBackground = true
                        controller.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (wasPlayingBeforeBackground) {
                        controller.play()
                        wasPlayingBeforeBackground = false
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
