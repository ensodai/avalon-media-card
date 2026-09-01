package org.ensodai.avalonmediacard

import org.ensodai.avalonmediacard.contract.version.CoreVersion
import kotlin.time.Clock
import kotlin.time.Instant

object ServerRuntimeInfo {
    val startTime: Instant = Clock.System.now()

    val serverVersion: String by lazy {
        ServerRuntimeInfo::class.java.`package`?.implementationVersion
            ?: System.getProperty("app.version")
            ?: CoreVersion.VERSION
    }

    fun getUptimeSeconds(): Long {
        return (Clock.System.now() - startTime).inWholeSeconds
    }
}
