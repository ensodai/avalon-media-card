@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.data


import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.rpc.AdminRpcService
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.koin.core.context.GlobalContext
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get

private val uploadScope = CoroutineScope(Dispatchers.Main)

actual fun selectAndUploadPlugin(onResult: (Boolean, String) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".jar"

    input.onchange = {
        val files = input.files
        if (files != null && files.length > 0) {
            val file = files[0]
            if (file != null) {
                onResult(true, "Reading file: ${file.name}...")
                readFileAndUpload(file, onResult)
            }
        }
    }
    input.click()
}

private fun readFileAndUpload(file: File, onResult: (Boolean, String) -> Unit) {
    val reader = FileReader()
    reader.onload = {
        val arrayBuffer = reader.result as org.khronos.webgl.ArrayBuffer
        val int8Array = Int8Array(arrayBuffer)
        val byteArray = ByteArray(int8Array.length) { i -> int8Array[i] }

        onResult(true, "Uploading plugin via RPC...")

        uploadScope.launch {
            try {
                val adminRpcService = GlobalContext.get().get<AdminRpcService>()
                val success = adminRpcService.uploadPlugin(file.name, byteArray)
                if (success) {
                    onResult(true, "Plugin ${file.name} successfully uploaded and activated!")
                } else {
                    onResult(false, "Error: server failed to load plugin")
                }
            } catch (e: Exception) {
                onResult(false, "RPC Error: ${e.message}")
            }
        }
    }
    reader.onerror = {
        onResult(false, "Error reading file in browser")
    }
    reader.readAsArrayBuffer(file)
}
