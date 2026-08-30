package org.ensodai.avalonmediacard

import kotlin.time.Clock
import kotlin.time.Instant

object ServerRuntimeInfo {
    val startTime: Instant = Clock.System.now()

    fun getUptimeSeconds(): Long {
        return (Clock.System.now() - startTime).inWholeSeconds
    }
}
