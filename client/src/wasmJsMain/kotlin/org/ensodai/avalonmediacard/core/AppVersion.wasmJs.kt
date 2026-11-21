package org.ensodai.avalonmediacard.core

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => { const m = document.querySelector(\"meta[name='avalon-build-hash']\"); return m ? (m.getAttribute('content') || '') : ''; }")
private external fun getMetaBuildHashJs(): String

actual fun getPlatformAppVersion(): String {
    val buildHash = runCatching { getMetaBuildHashJs() }.getOrNull()
    return if (!buildHash.isNullOrBlank() && buildHash != "dev-local") {
        "${AppVersion.VERSION_NAME} ($buildHash)"
    } else {
        AppVersion.VERSION_NAME
    }
}
