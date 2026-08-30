package org.ensodai.avalonmediacard.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
import org.ensodai.avalonmediacard.data.platformServerUrl

@Composable
fun ShimmerImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    shape: Shape = RectangleShape,
    isLoading: Boolean = false,
    errorIcon: ImageVector = Lucide.Film,
    errorIconTint: Color? = null,
    errorBackground: Color? = null
) {
    val tokenStorage = org.koin.compose.koinInject<org.ensodai.avalonmediacard.data.TokenStorage>()
    val serverUrlState by tokenStorage.serverUrl.collectAsState()

    val actualErrorTint = errorIconTint ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val actualErrorBg = errorBackground ?: MaterialTheme.colorScheme.surfaceVariant

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(shape)
                .shimmerPlaceholder(isLoading = true, shape = shape)
        )
    } else {
        val resolvedModel = remember(model, serverUrlState) {
            if (model is String && model.startsWith("/")) {
                val base = serverUrlState?.takeIf { it.isNotBlank() } ?: platformServerUrl
                StreamUrlResolver.resolveAbsoluteUrl(model, base)
            } else {
                model
            }
        }
        SubcomposeAsyncImage(
            model = resolvedModel,
            contentDescription = contentDescription,
            modifier = modifier.clip(shape),
            contentScale = contentScale,
            alignment = alignment
        ) {
            val state by painter.state.collectAsState()
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(400)).togetherWith(
                        fadeOut(tween(400))
                    )
                },
                label = "ImageLoadTransition"
            ) { targetState ->
                when (targetState) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerPlaceholder(isLoading = true, shape = shape)
                        )
                    }

                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(actualErrorBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = errorIcon,
                                contentDescription = null,
                                tint = actualErrorTint,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}
