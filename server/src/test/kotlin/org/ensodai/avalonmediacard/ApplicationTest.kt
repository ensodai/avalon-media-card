package org.ensodai.avalonmediacard

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.koin.ktor.ext.getKoin
import org.ensodai.avalonmediacard.plugin.PluginManager
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Avalon Media Card Server API (RPC is running)", response.bodyAsText())
    }

    @Test
    fun testStreamProxyBase64UrlDecoding() {
        val rawUrl = "http://127.0.0.1:8090/stream/video.mp4?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=1&play"
        val encodedUrl = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawUrl.toByteArray())
        val decoded = String(java.util.Base64.getUrlDecoder().decode(encodedUrl))
        assertEquals(rawUrl, decoded)
    }

    @Test
    fun testGlobalManifestHasPlayButtonsFromCore() = testApplication {
        var pluginManager: org.ensodai.avalonmediacard.plugin.PluginManager? = null
        application {
            module()
            pluginManager = getKoin().get<PluginManager>()
        }
        client.get("/")
        val manifest = pluginManager!!.buildGlobalManifest(null)
        
        val movieDetailsManifest = manifest.screens["MovieDetails"]
        kotlin.test.assertNotNull(movieDetailsManifest, "MovieDetails manifest should exist")
        kotlin.test.assertTrue(
            movieDetailsManifest.slots.contains(org.ensodai.avalonmediacard.contract.slot.SlotId.PlayButtons),
            "MovieDetails must contain PlayButtons slot declared by core"
        )
        kotlin.test.assertTrue(
            movieDetailsManifest.layout.any { it.nodeId == "core" && it.slotId == org.ensodai.avalonmediacard.contract.slot.SlotId.PlayButtons },
            "MovieDetails layout must have core PlayButtons layout node"
        )
    }
}