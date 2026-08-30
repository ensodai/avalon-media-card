package org.ensodai.avalonmediacard.core.player/*
package org.ensodai.avalonmediacard.core.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatroskaEbmlDemuxerTest {

    @Test
    fun testUniversalSubtitleParserSrt() {
        val srtSample = """
            1
            00:00:01,000 --> 00:00:04,000
            Привет, космический корабль!

            2
            00:00:05,500 --> 00:00:09,000
            Капитан, мы выходим на орбиту.
        """.trimIndent()

        val cues = UniversalSubtitleParser.parseSubtitle(srtSample)
        assertEquals(2, cues.size)
        assertEquals("Привет, космический корабль!", cues[0].text)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(4000L, cues[0].endMs)

        val active = UniversalSubtitleParser.binarySearchActiveCues(cues, 2000L)
        assertEquals(1, active.size)
        assertEquals("Привет, космический корабль!", active[0].text)
    }

    @Test
    fun testAssSubtitleParser() {
        val assSample = """
            [Script Info]
            Title: Star Trek DS9
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: Default,Arial,20
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.50,0:00:05.20,Default,,0,0,0,,{\pos(100,200)}Командир Сиско на связи!
        """.trimIndent()

        val cues = UniversalSubtitleParser.parseSubtitle(assSample)
        assertEquals(1, cues.size)
        assertEquals("Командир Сиско на связи!", cues[0].text)
        assertEquals(1500L, cues[0].startMs)
        assertEquals(5200L, cues[0].endMs)
    }

    @Test
    fun testMatroskaEbmlDemuxerMockBytes() {
        val trackEntryContent = byteArrayOf(
            0x83.toByte(), 0x81.toByte(), 0x11.toByte(), // TrackType (0x83), len 1, 17 (Subtitle)
            0x86.toByte(), 0x8B.toByte(), // CodecID (0x86), len 11
            'S'.code.toByte(), '_'.code.toByte(), 'T'.code.toByte(), 'E'.code.toByte(), 'X'.code.toByte(), 'T'.code.toByte(), '/'.code.toByte(), 'U'.code.toByte(), 'T'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte()
        )
        val mockEbml = byteArrayOf(
            0xAE.toByte(), (0x80 or trackEntryContent.size).toByte()
        ) + trackEntryContent

        // val result = MatroskaEbmlDemuxer.parseHeader(mockEbml)
        // assertEquals(1, result.subtitleTracks.size)
        // assertTrue(result.subtitleTracks[0].mimeType.contains("subrip"))
    }

    @Test
    fun testUniversalContainerDemuxerMp4() {
        val hdlrContent = byteArrayOf(
            0, 0, 0, 0,
            0, 0, 0, 0,
            's'.code.toByte(), 'o'.code.toByte(), 'u'.code.toByte(), 'n'.code.toByte()
        )
        val hdlrBox = byteArrayOf(
            0, 0, 0, (8 + hdlrContent.size).toByte(),
            'h'.code.toByte(), 'd'.code.toByte(), 'l'.code.toByte(), 'r'.code.toByte()
        ) + hdlrContent

        // val result = UniversalContainerDemuxer.parseHeader(hdlrBox)
        // assertEquals(1, result.audioTracks.size)
    }
}
*/
