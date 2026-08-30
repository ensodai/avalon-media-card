package org.ensodai.avalonmediacard.security

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Валидатор IP-адресов, реализующий бинарное побитовое сравнение с CIDR-масками.
 * Защищает от SSRF, обращений к Bogon IPs, Cloud Metadata (169.254.169.254), Loopback и интранету.
 */
object SsrfIpValidator {

    // Таблица блокируемых IPv4 подсетей (RFC 1918, RFC 5735, RFC 3927, Bogon)
    private val blockedIpv4Cidrs: List<Pair<ByteArray, Int>> = listOf(
        byteArrayOf(0, 0, 0, 0) to 8,                                // "This" network (0.0.0.0/8)
        byteArrayOf(10, 0, 0, 0) to 8,                               // Private-use (10.0.0.0/8)
        byteArrayOf(100, 64, 0, 0) to 10,                            // Carrier-grade NAT (100.64.0.0/10)
        byteArrayOf(127, 0, 0, 0) to 8,                              // Loopback (127.0.0.0/8)
        byteArrayOf(169.toByte(), 254.toByte(), 0, 0) to 16,                  // Link-local & Cloud Metadata (169.254.0.0/16)
        byteArrayOf(172.toByte(), 16, 0, 0) to 12,                            // Private-use (172.16.0.0/12)
        byteArrayOf(192.toByte(), 168.toByte(), 0, 0) to 16,                  // Private-use (192.168.0.0/16)
        byteArrayOf(198.toByte(), 18, 0, 0) to 15,                   // Benchmark testing (198.18.0.0/15)
        byteArrayOf(224.toByte(), 0, 0, 0) to 4,                     // Multicast (224.0.0.0/4)
        byteArrayOf(240.toByte(), 0, 0, 0) to 4,                     // Future use / Reserved (240.0.0.0/4)
        byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()) to 32 // Broadcast
    )

    /**
     * Проверяет, является ли IP-адрес публичным и безопасным для исходящего прокси-запроса.
     */
    fun isAllowed(inetAddress: InetAddress): Boolean {
        // Базовые проверки JVM
        if (inetAddress.isAnyLocalAddress ||
            inetAddress.isLoopbackAddress ||
            inetAddress.isLinkLocalAddress ||
            inetAddress.isSiteLocalAddress ||
            inetAddress.isMulticastAddress
        ) {
            return false
        }

        val addressBytes = inetAddress.address

        // Строгая проверка IPv4
        if (inetAddress is Inet4Address) {
            return !blockedIpv4Cidrs.any { (subnet, prefix) ->
                matchBytes(addressBytes, subnet, prefix)
            }
        }

        // Проверка IPv6
        if (inetAddress is Inet6Address) {
            // IPv4-compatible (::x.x.x.x)
            if (inetAddress.isIPv4CompatibleAddress) return false

            // IPv4-mapped IPv6 (::ffff:0:0/96), например ::ffff:127.0.0.1
            val ipv4MappedPrefix = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 255.toByte(), 255.toByte())
            if (matchBytes(addressBytes, ipv4MappedPrefix, 96)) {
                val ipv4Part = addressBytes.copyOfRange(12, 16)
                val mappedInet4 = InetAddress.getByAddress(ipv4Part)
                return isAllowed(mappedInet4)
            }

            // Unique Local Addresses (ULA) - fc00::/7
            if (matchBytes(addressBytes, byteArrayOf(252.toByte(), 0), 7)) {
                return false
            }

            // Link-local unicast - fe80::/10
            if (matchBytes(addressBytes, byteArrayOf(254.toByte(), 128.toByte()), 10)) {
                return false
            }
        }

        return true
    }

    private fun matchBytes(raw: ByteArray, subnet: ByteArray, prefix: Int): Boolean {
        var bits = prefix
        var byteIndex = 0

        while (bits >= 8) {
            if (raw[byteIndex] != subnet[byteIndex]) return false
            byteIndex++
            bits -= 8
        }

        if (bits > 0) {
            val mask = (0xFF00 shr bits) and 0xFF
            val rawVal = raw[byteIndex].toInt() and mask
            val subnetVal = subnet[byteIndex].toInt() and mask
            if (rawVal != subnetVal) return false
        }
        return true
    }
}
