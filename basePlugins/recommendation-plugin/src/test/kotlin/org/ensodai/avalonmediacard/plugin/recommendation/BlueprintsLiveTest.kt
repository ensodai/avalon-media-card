package org.ensodai.avalonmediacard.plugin.recommendation

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterContext
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.SemanticSegment
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueprintsLiveTest {

    private val allBlueprints = listOf(
        SerendipityTropeIsolationBlueprint,
        TempAfterWorkDecompressBlueprint,
        TempMorningRushBlueprint,
        TempLunchBreakBlueprint,
        TempMidnightMelancholyBlueprint,
        TempSundayCozyBlueprint,
        TempWeekendEpicBingeBlueprint,
        BingeWatchingWeekendBlueprint,
        AestheticStudioVibeBlueprint,
        AestheticAuteurThemesBlueprint,
        AestheticDecadeNostalgiaBlueprint,
        AestheticIndieDarlingsBlueprint,
        AestheticBlackAndWhiteBlueprint,
        AestheticForeignGemsBlueprint,
        MoodDarkComedyMixBlueprint,
        MoodClaustrophobiaBlueprint,
        MoodMindBenderBlueprint,
        MoodUnapologeticGritBlueprint,
        MoodHeartwarmingBlueprint,
        MoodAdrenalineSpikeBlueprint,
        DirectorStudioSynergyBlueprint,
        NostalgiaTriggerBlueprint,
        ActorMoodSynergyBlueprint,
        SerendipityActorPivotBlueprint,
        SerendipityGenreBendBlueprint,
        SerendipityDecadeSwapBlueprint,
        SerendipityAnimationTrapBlueprint,
        SerendipityBlindSpotBlueprint,
        EchoChamberBreakerBlueprint,
        MicroNicheExplorationBlueprint,
        SocialHiddenGemsBlueprint,
        SocialTrendingNowBlueprint,
        SocialUpcomingHypeBlueprint,
        SocialCriticsChoiceBlueprint,
        SocialGuiltyPleasureBlueprint,
        SocialFranchiseBingeBlueprint,
        ExploitBecauseYouWatchedBlueprint,
        ExploitCurrentObsessionBlueprint,
        ExploitActorBingeBlueprint,
        ExploitDirectorDeepDiveBlueprint,
        ExploitGenreMastersBlueprint
    )

    private fun getTmdbToken(): String {
        val fromEnv = System.getenv("TMDB_READ_TOKEN")
        if (!fromEnv.isNullOrBlank()) return fromEnv.trim().removeSurrounding("\"")

        val candidates = listOf(
            File(".env"),
            File("../../.env"),
            File("../../../.env")
        )
        for (envFile in candidates) {
            if (envFile.exists()) {
                val tokenLine = envFile.readLines().firstOrNull { it.startsWith("TMDB_READ_TOKEN=") }
                if (tokenLine != null) {
                    val token = tokenLine.substringAfter("=").trim().removeSurrounding("\"")
                    if (token.isNotEmpty()) return token
                }
            }
        }
        return ""
    }

    private fun buildSaturatedVector(
        genreLevel: Double,
        keywordLevel: Double,
        fastPacingLevel: Double,
        aLevel: Double
    ): AffinityVector {
        val allGenres = (1..1000).associate { it.toString() to genreLevel }.toMutableMap()
        allGenres["16"] = aLevel
        val allKeywords = (1..5000).associate { it.toString() to keywordLevel }
        val allDirectors = (1..5000).associate { it.toString() to genreLevel }
        val allActors = (1..5000).associate { it.toString() to genreLevel }
        val allCompanies = (1..5000).associate { it.toString() to genreLevel }
        val allPacing = mapOf(
            "slow_epic" to genreLevel,
            "fast_paced" to genreLevel,
            "fast" to fastPacingLevel,
            "slow" to (1.0 - fastPacingLevel)
        )
        val allEras = mapOf("1990s" to genreLevel, "2000s" to genreLevel, "2010s" to genreLevel, "2020s" to genreLevel)
        val moodWeights = mapOf(
            "laugh" to keywordLevel,
            "tension" to keywordLevel,
            "dark" to keywordLevel,
            "joy" to keywordLevel,
            "intellectual" to keywordLevel,
            "adrenaline" to keywordLevel
        )

        return AffinityVector(
            genreWeights = allGenres,
            keywordWeights = allKeywords,
            directorWeights = allDirectors,
            actorWeights = allActors,
            companyWeights = allCompanies,
            pacingWeights = allPacing,
            eraWeights = allEras,
            moodWeights = moodWeights + allKeywords,
            recentWatchedIds = listOf("1", "2", "3"),
            sessionBingeVector = mapOf("878" to 0.8)
        )
    }

    @Test
    fun testAllBlueprintsGenerateValidTmdbQueries() {
        // ПОМЕТКА: В данный момент (на момент написания) все 41 блюпринт 
        // работают корректно, генерируют валидные параметры и отдают 200 OK от реального TMDB API.

        val token = getTmdbToken()
        if (token.isBlank()) {
            println("Skipping BlueprintsLiveTest: TMDB_READ_TOKEN not found in environment or .env")
            return
        }

        var successCount = 0
        var failCount = 0
        val failedBlueprints = mutableListOf<String>()

        for (blueprint in allBlueprints) {
            var segment: SemanticSegment? = null

            // Brute force saturation levels and hours
            val levels = listOf(1.0 to 1.0, 0.5 to 1.0, 1.0 to 0.5, 0.3 to 0.3, 1.0 to 0.1)
            for ((gLevel, kLevel) in levels) {
                for (fLevel in listOf(0.1, 0.9)) {
                    for (aLevel in listOf(0.1, 0.9)) {
                        val vector = buildSaturatedVector(gLevel, kLevel, fLevel, aLevel)
                        for (hour in 0..23) {
                            val context = InterpreterContext(
                                affinityVector = vector,
                                topGenres = listOf("878", "28", "12", "18"),
                                topKeywords = listOf("123", "456", "789"),
                                localHour = hour,
                                localizedGenres = mapOf("878" to "Фантастика", "18" to "Драма"),
                                isWeekend = true
                            )
                            segment = blueprint.evaluate(context)
                            if (segment != null) break
                        }
                        if (segment != null) break
                    }
                    if (segment != null) break
                }
                if (segment != null) break
            }

            // Try weekday if weekend failed
            if (segment == null) {
                val levels = listOf(1.0 to 1.0, 0.5 to 1.0, 1.0 to 0.5, 0.3 to 0.3, 1.0 to 0.1)
                for ((gLevel, kLevel) in levels) {
                    for (fLevel in listOf(0.1, 0.9)) {
                        for (aLevel in listOf(0.1, 0.9)) {
                            val vector = buildSaturatedVector(gLevel, kLevel, fLevel, aLevel)
                            for (hour in 0..23) {
                                val context = InterpreterContext(
                                    affinityVector = vector,
                                    topGenres = listOf("878", "28", "12", "18"),
                                    topKeywords = listOf("123", "456", "789"),
                                    localHour = hour,
                                    localizedGenres = mapOf("878" to "Фантастика", "18" to "Драма"),
                                    isWeekend = false
                                )
                                segment = blueprint.evaluate(context)
                                if (segment != null) break
                            }
                            if (segment != null) break
                        }
                        if (segment != null) break
                    }
                    if (segment != null) break
                }
            }

            assertNotNull(
                segment,
                "Blueprint ${blueprint.blueprintId} could not be evaluated with saturated vector in any time condition"
            )

            // Make the real HTTP Request
            val typeStr = if (segment.targetType == EntityType.TV) "tv" else "movie"
            var urlString = "https://api.themoviedb.org/3/discover/$typeStr?language=ru-RU"
            segment.queryParams.forEach { (k, v) ->
                val encodedVal = java.net.URLEncoder.encode(v, "UTF-8")
                urlString += "&$k=$encodedVal"
            }

            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    println("[OK] ${blueprint.blueprintId}: $urlString")
                    successCount++
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    println("[ERROR] ${blueprint.blueprintId} returned $responseCode: $errorStream (URL: $urlString)")
                    failCount++
                    failedBlueprints.add(blueprint.blueprintId)
                }
            } catch (e: Exception) {
                println("[ERROR] ${blueprint.blueprintId} network exception: ${e.message}")
                failCount++
                failedBlueprints.add(blueprint.blueprintId)
            }
        }

        println("TMDB Live Test Complete. Success: $successCount, Failed: $failCount")
        assertTrue(failCount == 0, "Some blueprints generated invalid TMDB parameters: $failedBlueprints")
    }
}
