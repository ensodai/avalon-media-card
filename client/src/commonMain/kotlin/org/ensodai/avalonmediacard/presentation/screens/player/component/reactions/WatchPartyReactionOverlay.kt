package org.ensodai.avalonmediacard.presentation.screens.player.component.reactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.presentation.screens.player.model.WatchPartyReaction
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class ReactionParticle(
    val id: Long,
    val emoji: String,
    val startXOffset: Dp,
    val maxRiseHeight: Dp,
    val swayAmplitude: Dp,
    val swayCycles: Float,
    val rotationDeg: Float,
    val targetScale: Float
)

/**
 * Оверлей всплывающих реакций эмодзи (фонтан в левом нижнем углу экрана).
 *
 * Полностью прозрачен для пользовательских жестов и кликов (не перехватывает ввод).
 * Анимирует каждую частицу аппаратно через [graphicsLayer] с плавной синусоидальной
 * траекторией, пружинящим рождением и плавным затуханием вверху экрана.
 */
@Composable
fun WatchPartyReactionOverlay(
    lastReaction: WatchPartyReaction?,
    modifier: Modifier = Modifier
) {
    val activeParticles = remember { mutableStateListOf<ReactionParticle>() }

    LaunchedEffect(lastReaction?.id) {
        if (lastReaction != null) {
            val random = Random(lastReaction.id)
            val particle = ReactionParticle(
                id = lastReaction.id,
                emoji = lastReaction.emoji,
                startXOffset = (28 + random.nextInt(48)).dp,
                maxRiseHeight = (280 + random.nextInt(120)).dp,
                swayAmplitude = (12 + random.nextInt(16)).dp * (if (random.nextBoolean()) 1f else -1f),
                swayCycles = 1.0f + random.nextFloat() * 1.0f,
                rotationDeg = (random.nextFloat() - 0.5f) * 24f,
                targetScale = 1.0f + random.nextFloat() * 0.35f
            )
            // Ограничиваем максимальное количество одновременных частиц, чтобы не забивать память при спаме
            if (activeParticles.size > 50) {
                activeParticles.removeAt(0)
            }
            activeParticles.add(particle)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        activeParticles.forEach { particle ->
            key(particle.id) {
                ReactionParticleItem(
                    particle = particle,
                    onFinished = {
                        activeParticles.removeAll { it.id == particle.id }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionParticleItem(
    particle: ReactionParticle,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2300, easing = LinearEasing)
        )
        onFinished()
    }

    val currentProgress = progress.value

    // Траектория и эффекты:
    // 1. Появление: быстрый pop-in (0f -> 0.15f)
    val scale = when {
        currentProgress < 0.15f -> {
            val pop = currentProgress / 0.15f
            0.3f + (particle.targetScale * 1.15f - 0.3f) * pop
        }
        currentProgress < 0.25f -> {
            val settle = (currentProgress - 0.15f) / 0.10f
            particle.targetScale * 1.15f - (particle.targetScale * 0.15f) * settle
        }
        else -> particle.targetScale
    }

    // 2. Плавный fade-out в финальной трети (0.7f -> 1.0f)
    val alpha = when {
        currentProgress < 0.70f -> 1.0f
        else -> (1.0f - (currentProgress - 0.70f) / 0.30f).coerceIn(0f, 1f)
    }

    // 3. Синусоидальное покачивание по горизонтали
    val swayPx = sin(currentProgress * 2f * PI.toFloat() * particle.swayCycles)

    Box(
        modifier = Modifier.graphicsLayer {
            val riseDistancePx = particle.maxRiseHeight.toPx()
            val startXPx = particle.startXOffset.toPx()
            val amplitudePx = particle.swayAmplitude.toPx()

            translationY = -currentProgress * riseDistancePx - 100.dp.toPx()
            translationX = startXPx + swayPx * amplitudePx
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            rotationZ = particle.rotationDeg * currentProgress
        }
    ) {
        Text(
            text = particle.emoji,
            fontSize = 32.sp
        )
    }
}
