package org.ensodai.avalonmediacard.plugins.torrserver.parsers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

@Serializable
data class TestCaseItem(
    val root: String = "",
    val path: String,
    val expectedSeasons: List<Int> = emptyList(),
    val expectedEpisodes: List<Int> = emptyList(),
    val isAbsolute: Boolean = false,
    val description: String = ""
)

class EpisodeMatcherStressTest {

    private val matcher = EpisodeMatcher()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val scratchDir = File(System.getProperty("java.io.tmpdir"), "scratch")

    private fun runSuite(filename: String) {
        val file = File(scratchDir, filename)
        if (!file.exists()) {
            println("⚠️ Test suite file ${file.absolutePath} not found yet, skipping.")
            return
        }

        val content = file.readText()
        val testCases = json.decodeFromString<List<TestCaseItem>>(content)

        println("\n==========================================================================")
        println("RUNNING SUITE: $filename (${testCases.size} test cases)")
        println("==========================================================================")

        var passed = 0
        var failed = 0
        val failures = mutableListOf<String>()

        for ((index, tc) in testCases.withIndex()) {
            val resultMap = matcher.parseBatch(tc.root, listOf(tc.path))
            val result = resultMap[tc.path]

            var matchOk = false
            when (result) {
                is MappingResult.Success -> {
                    val seasonsOk = if (tc.expectedSeasons.isNotEmpty()) {
                        result.seasons.sorted() == tc.expectedSeasons.sorted()
                    } else true
                    val episodesOk = if (tc.expectedEpisodes.isNotEmpty()) {
                        result.episodes.sorted() == tc.expectedEpisodes.sorted()
                    } else true
                    matchOk = seasonsOk && episodesOk
                }
                is MappingResult.Partial -> {
                    if (tc.expectedSeasons.isEmpty() || tc.isAbsolute) {
                        val episodesOk = if (tc.expectedEpisodes.isNotEmpty()) {
                            result.episodes.sorted() == tc.expectedEpisodes.sorted()
                        } else true
                        matchOk = episodesOk
                    } else {
                        matchOk = false
                    }
                }
                is MappingResult.Failed, null -> {
                    matchOk = tc.expectedEpisodes.isEmpty() && tc.expectedSeasons.isEmpty()
                }
            }

            if (matchOk) {
                passed++
            } else {
                failed++
                val msg = """
                    ❌ [FAIL #$index] '${tc.description}'
                       Root:     '${tc.root}'
                       Path:     '${tc.path}'
                       Expected: S=${tc.expectedSeasons}, E=${tc.expectedEpisodes}, abs=${tc.isAbsolute}
                       Actual:   $result
                """.trimIndent()
                failures.add(msg)
                println(msg)
            }
        }

        println("\nSUITE SUMMARY for $filename: Passed: $passed / ${testCases.size} (Failed: $failed)")
        if (failed > 0) {
            println("⚠️ Failures breakdown:")
            failures.take(15).forEach { println(it) }
        }
        assertTrue(failed == 0, "Suite $filename had $failed failures out of ${testCases.size}")
    }

    @Test
    fun testSuite1SceneTorrents() {
        runSuite("tests_agent1_scene.json")
    }

    @Test
    fun testSuite2RutubeVk() {
        runSuite("tests_agent2_rutube_vk.json")
    }

    @Test
    fun testSuite3AnimeAsian() {
        runSuite("tests_agent3_anime_asian.json")
    }

    @Test
    fun testSuite4TrickyTitles() {
        runSuite("tests_agent4_tricky_titles.json")
    }

    @Test
    fun testSuite5FolderInheritance() {
        runSuite("tests_agent5_folder_inheritance.json")
    }
}
