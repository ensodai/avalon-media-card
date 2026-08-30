package org.ensodai.avalonmediacard.data


actual fun selectAndUploadPlugin(onResult: (Boolean, String) -> Unit) {
    onResult(false, "Not supported on this platform")
}
