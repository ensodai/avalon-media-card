package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent

import org.ensodai.avalonmediacard.contract.parsers.GarbageCollector
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem

data class AlignedEpisodeResult(
    val episodeNumber: Int,
    val matchedEpisode: EpisodeItem?
)

object EpisodeAligner {

    data class FileInput(
        val filePath: String,
        val parsedSeason: Int,
        val parsedEpisodes: List<Int>,
        val isManual: Boolean = false
    )

    private val TECH_TAGS_REGEX = Regex(
        """\b(?:web-dl|webdl|bluray|bdrip|hevc|h264|x265|x264|dvdrip|rus|eng|subs?|dub|multi|casstudio|1080p|720p|2160p|4k|dd5\.1|ac3|aac|dts|remux|hdtv|hdtvrip)\b""",
        RegexOption.IGNORE_CASE
    )

    private val SEASON_EP_MARKER_REGEX = Regex(
        """(?i)(?:[Ss]\d{1,2}[._\-\s]*[Ee]\d{1,3}(?:[._\-\s]*[Ee]\d{1,3})?|\b\d{1,2}[xXхХ]\d{1,3}\b|(?:сезон\s*\d+\s*)?(?:серия|эпизод|выпуск)\s*\d+)"""
    )

    private val CLEAN_ALPHANUM_REGEX = Regex("""[^a-z0-9а-яё]""")

    fun extractTitleCandidate(filePath: String): String {
        val fileName = filePath.substringAfterLast('/').substringAfterLast('\\')
        var name = fileName.substringBeforeLast('.')
        name = TECH_TAGS_REGEX.replace(name, " ")
        name = GarbageCollector.cleanHeavy(name)
        val epMatch = SEASON_EP_MARKER_REGEX.find(name)
        if (epMatch != null) {
            name = name.substring(epMatch.range.last + 1)
        }
        return name.trim(' ', '-', '.', '_')
    }

    fun normalize(s: String): String {
        return s.lowercase().replace(CLEAN_ALPHANUM_REGEX, "")
    }

    private fun isTitleMatch(fileTitleNorm: String, tmdbTitleNorm: String): Boolean {
        if (fileTitleNorm.isBlank() || tmdbTitleNorm.isBlank()) return false
        if (fileTitleNorm.length < 3 || tmdbTitleNorm.length < 3) return false

        if (fileTitleNorm == tmdbTitleNorm) return true
        if (fileTitleNorm.startsWith(tmdbTitleNorm) && tmdbTitleNorm.length >= 4) return true
        if (tmdbTitleNorm.startsWith(fileTitleNorm) && fileTitleNorm.length >= 4) return true
        if (fileTitleNorm.contains(tmdbTitleNorm) && tmdbTitleNorm.length >= 5) return true
        if (tmdbTitleNorm.contains(fileTitleNorm) && fileTitleNorm.length >= 5) return true

        return false
    }

    fun alignSeason(
        season: Int,
        files: List<FileInput>,
        tmdbEpisodesRu: List<EpisodeItem>,
        tmdbEpisodesEn: List<EpisodeItem> = emptyList()
    ): Map<String, AlignedEpisodeResult> {
        val seasonFiles = files.filter { it.parsedSeason == season }
        if (seasonFiles.isEmpty()) return emptyMap()

        if (tmdbEpisodesRu.isEmpty() && tmdbEpisodesEn.isEmpty()) {
            return seasonFiles.associate { file ->
                val epNum = file.parsedEpisodes.firstOrNull() ?: 1
                file.filePath to AlignedEpisodeResult(epNum, null)
            }
        }

        val allTmdbEpisodes = (tmdbEpisodesRu + tmdbEpisodesEn)
            .distinctBy { it.episodeNumber }
            .sortedBy { it.episodeNumber }

        val enByNum = tmdbEpisodesEn.associateBy { it.episodeNumber }
        val ruByNum = tmdbEpisodesRu.associateBy { it.episodeNumber }

        val sortedFiles = seasonFiles.sortedWith(
            compareBy(
                { it.parsedEpisodes.firstOrNull() ?: 0 },
                { it.filePath }
            )
        )

        val result = mutableMapOf<String, AlignedEpisodeResult>()
        val claimedEpisodeNumbers = mutableSetOf<Int>()

        // 1. Preserve manual mappings
        for (file in sortedFiles) {
            if (file.isManual && file.parsedEpisodes.isNotEmpty()) {
                val manualEpNum = file.parsedEpisodes.first()
                val matchedEp = ruByNum[manualEpNum] ?: enByNum[manualEpNum]
                result[file.filePath] = AlignedEpisodeResult(manualEpNum, matchedEp)
                claimedEpisodeNumbers.add(manualEpNum)
            }
        }

        // 2. Title matching for remaining files
        val remainingAfterManual = sortedFiles.filter { it.filePath !in result }
        for (file in remainingAfterManual) {
            val candidate = extractTitleCandidate(file.filePath)
            val candidateNorm = normalize(candidate)
            if (candidateNorm.length >= 3) {
                val match = allTmdbEpisodes.firstOrNull { ep ->
                    ep.episodeNumber !in claimedEpisodeNumbers && (
                        isTitleMatch(candidateNorm, normalize(ep.name)) ||
                        isTitleMatch(candidateNorm, normalize(enByNum[ep.episodeNumber]?.name ?: "")) ||
                        isTitleMatch(candidateNorm, normalize(ruByNum[ep.episodeNumber]?.name ?: ""))
                    )
                }
                if (match != null) {
                    val primaryEp = ruByNum[match.episodeNumber] ?: match
                    result[file.filePath] = AlignedEpisodeResult(match.episodeNumber, primaryEp)
                    claimedEpisodeNumbers.add(match.episodeNumber)
                }
            }
        }

        // 3. Sequential index matching if count matches or direct numbers
        val remainingAfterTitle = sortedFiles.filter { it.filePath !in result }
        if (remainingAfterTitle.isNotEmpty()) {
            val availableTmdb = allTmdbEpisodes.filter { it.episodeNumber !in claimedEpisodeNumbers }

            if (remainingAfterTitle.size == availableTmdb.size) {
                // One-to-one sequence alignment (handles TMDB gaps like Star Trek Enterprise season 1)
                for (i in remainingAfterTitle.indices) {
                    val file = remainingAfterTitle[i]
                    val ep = availableTmdb[i]
                    val primaryEp = ruByNum[ep.episodeNumber] ?: ep
                    result[file.filePath] = AlignedEpisodeResult(ep.episodeNumber, primaryEp)
                    claimedEpisodeNumbers.add(ep.episodeNumber)
                }
            } else {
                // Number-based alignment with gap fallback
                for (file in remainingAfterTitle) {
                    val parsedNum = file.parsedEpisodes.firstOrNull() ?: 1
                    val directMatch = availableTmdb.firstOrNull { it.episodeNumber == parsedNum }
                    if (directMatch != null) {
                        val primaryEp = ruByNum[directMatch.episodeNumber] ?: directMatch
                        result[file.filePath] = AlignedEpisodeResult(directMatch.episodeNumber, primaryEp)
                        claimedEpisodeNumbers.add(directMatch.episodeNumber)
                    } else {
                        // Find first available TMDB episode >= parsedNum, or first available
                        val fallbackEp = availableTmdb.firstOrNull { it.episodeNumber >= parsedNum && it.episodeNumber !in claimedEpisodeNumbers }
                            ?: availableTmdb.firstOrNull { it.episodeNumber !in claimedEpisodeNumbers }

                        if (fallbackEp != null) {
                            val primaryEp = ruByNum[fallbackEp.episodeNumber] ?: fallbackEp
                            result[file.filePath] = AlignedEpisodeResult(fallbackEp.episodeNumber, primaryEp)
                            claimedEpisodeNumbers.add(fallbackEp.episodeNumber)
                        } else {
                            result[file.filePath] = AlignedEpisodeResult(parsedNum, ruByNum[parsedNum] ?: enByNum[parsedNum])
                        }
                    }
                }
            }
        }

        return result
    }
}
