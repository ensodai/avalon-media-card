package org.ensodai.avalonmediacard.data

import kotlinx.browser.window

actual var platformServerUrl: String = run {
    try {
        val location = window.location
        val host = location.host
        if (host.isNotBlank()) {
            val protocol = if (location.protocol == "https:") "wss:" else "ws:"
            "$protocol//$host/api/rpc"
        } else {
            "ws://localhost:8080/api/rpc"
        }
    } catch (_: Throwable) {
        "ws://localhost:8080/api/rpc"
    }
}
