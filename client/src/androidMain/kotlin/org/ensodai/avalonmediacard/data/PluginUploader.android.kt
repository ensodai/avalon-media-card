package org.ensodai.avalonmediacard.data

actual fun selectAndUploadPlugin(onResult: (Boolean, String) -> Unit) {
    onResult(false, "Plugin uploading from device is not supported on Android")
}
