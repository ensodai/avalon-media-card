package org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.GenreTranslationLayer
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterBlueprint
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterContext
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.SemanticSegment

@DslMarker
annotation class BlueprintDslMarker

class RejectBlueprintException : Exception("Blueprint rejected gracefully")

@BlueprintDslMarker
class BlueprintBuilder(val context: InterpreterContext, val id: String) {
    var computedScore: Double = 0.0

    fun calculateRelevance(
        baseAffinity: Double,
        temporalMultiplier: Double = 1.0,
        serendipityMultiplier: Double = 1.0,
        saturationPenalty: Double = 1.0
    ): Double {
        val raw = baseAffinity * temporalMultiplier * serendipityMultiplier * saturationPenalty
        return (raw / (raw + 0.5)).coerceIn(0.0, 1.0)
    }

    fun reject(): Nothing {
        throw RejectBlueprintException()
    }

    fun condition(block: () -> Boolean) {
        if (!block()) reject()
    }

    fun score(block: () -> Double) {
        computedScore = block()
    }

    fun t(key: String, vararg args: Any): String = context.i18n.tForLocale(context.locale, key, *args)

    fun segment(block: SegmentBuilder.() -> Unit): SemanticSegment {
        val segmentBuilder = SegmentBuilder(context)
        segmentBuilder.block()

        // Автоматическая трансляция жанров под целевой медиа-тип (MOVIE или TV)
        val finalParams = segmentBuilder.params.mapValues { (key, value) ->
            if (key == "with_genres" || key == "without_genres") {
                GenreTranslationLayer.translateToTarget(value, segmentBuilder.type)
            } else {
                value
            }
        }

        return SemanticSegment(
            blueprintId = id,
            displayTitle = segmentBuilder.title,
            displaySubtitle = segmentBuilder.subtitle,
            targetType = segmentBuilder.type,
            queryParams = finalParams,
            relevanceScore = computedScore,
            visualLayout = segmentBuilder.layout,
            mediaIds = segmentBuilder.mediaIds
        )
    }
}

@BlueprintDslMarker
class SegmentBuilder(val context: InterpreterContext) {
    var title: String = ""
    var subtitle: String = ""
    var type: EntityType = EntityType.MOVIE
    var layout: SectionType = SectionType.CAROUSEL_POSTERS
    var params: Map<String, String> = emptyMap()
    var mediaIds: List<MediaKey> = emptyList()

    fun t(key: String, vararg args: Any): String = context.i18n.tForLocale(context.locale, key, *args)
}

/**
 * Основная точка входа для создания DSL Blueprint.
 */
fun blueprint(id: String, block: BlueprintBuilder.() -> SemanticSegment): InterpreterBlueprint {
    return object : InterpreterBlueprint {
        override val blueprintId = id
        override fun evaluate(context: InterpreterContext): SemanticSegment? {
            val builder = BlueprintBuilder(context, id)
            return try {
                builder.block()
            } catch (e: RejectBlueprintException) {
                null
            } catch (e: Exception) {
                // Логируем непредвиденную ошибку, но не роняем весь генератор
                null
            }
        }
    }
}
