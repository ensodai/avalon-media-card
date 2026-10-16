package org.ensodai.avalonmediacard.service

import org.junit.Test
import kotlin.test.assertEquals

class PluginProxyServiceTest {

    @Test
    fun testSanitizeHost() {
        assertEquals("192.168.1.100", PluginProxyAuthenticator.sanitizeHost("socks5://192.168.1.100"))
        assertEquals("192.168.1.100", PluginProxyAuthenticator.sanitizeHost("socks5h://192.168.1.100/"))
        assertEquals("proxy.example.com", PluginProxyAuthenticator.sanitizeHost(" http://proxy.example.com:8080/ "))
    }
}

