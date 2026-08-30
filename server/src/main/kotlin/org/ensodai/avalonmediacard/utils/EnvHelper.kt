package org.ensodai.avalonmediacard.utils

import java.io.File

object EnvHelper {
    fun getEnv(key: String): String? {
        val envValue = System.getenv(key)
        if (envValue != null) return envValue

        try {
            var dir = File(".").absoluteFile
            for (i in 0..4) {
                val file = File(dir, ".env")
                if (file.exists()) {
                    val value = readKeyFromEnvFile(file, key)
                    if (value != null) return value
                }
                dir = dir.parentFile ?: break
            }
        } catch (e: Exception) {
            // Игнорируем
        }
        return null
    }

    private fun readKeyFromEnvFile(file: File, key: String): String? {
        file.readLines().forEach { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2 && parts[0].trim() == key) {
                return parts[1].trim()
            }
        }
        return null
    }
}
