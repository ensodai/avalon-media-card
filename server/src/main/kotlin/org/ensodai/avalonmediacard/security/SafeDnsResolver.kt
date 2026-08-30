package org.ensodai.avalonmediacard.security

import okhttp3.Dns
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Резолвер DNS, внедряемый в OkHttp HttpClient для предотвращения SSRF и атак DNS Rebinding (TOCTOU).
 * Проверяет разрешенные IP-адреса непосредственно перед открытием TCP-сокета.
 */
@Single
class SafeDnsResolver : Dns {

    private val logger = LoggerFactory.getLogger(SafeDnsResolver::class.java)

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.isEmpty()) {
            throw UnknownHostException("DNS resolution failed for hostname: $hostname")
        }

        val safeAddresses = addresses.filter { SsrfIpValidator.isAllowed(it) }
        if (safeAddresses.isEmpty()) {
            logger.warn("🚨 SSRF Blocked: Hostname '{}' resolved only to forbidden/private IPs: {}", hostname, addresses)
            throw UnknownHostException("SSRF Protection: Hostname $hostname resolved to restricted or private IP address")
        }

        return safeAddresses
    }
}
