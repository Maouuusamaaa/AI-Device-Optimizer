package com.maouuusama.ai.device.optimizer.localai

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class QwenModelDownloader(private val context: Context) {
    fun isInstalled(): Boolean {
        val file = QwenLocalModel.file(context)
        return file.isFile &&
            file.length() == QwenLocalModel.EXPECTED_SIZE_BYTES &&
            sha256(file) == QwenLocalModel.EXPECTED_SHA256
    }

    fun download(progress: (Long, Long) -> Unit = { _, _ -> }) {
        val directory = QwenLocalModel.file(context).parentFile ?: error("Model directory unavailable")
        check(directory.mkdirs() || directory.isDirectory)

        val target = QwenLocalModel.file(context)
        val partial = QwenLocalModel.partialFile(context)
        var existing = if (partial.isFile) partial.length() else 0L

        val connection = (URL(QwenLocalModel.DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
            if (existing > 0L) setRequestProperty("Range", "bytes=$existing-")
        }

        try {
            val code = connection.responseCode
            val append = existing > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (!append) existing = 0L
            if (code !in 200..299 && !append) error("Model download failed with HTTP $code")

            val total = when {
                append -> existing + connection.contentLengthLong.coerceAtLeast(0L)
                connection.contentLengthLong > 0L -> connection.contentLengthLong
                else -> QwenLocalModel.EXPECTED_SIZE_BYTES
            }

            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = existing
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        progress(downloaded, total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        check(partial.length() == QwenLocalModel.EXPECTED_SIZE_BYTES) {
            "Unexpected model size: ${partial.length()}"
        }
        check(sha256(partial) == QwenLocalModel.EXPECTED_SHA256) {
            "Model SHA-256 verification failed"
        }
        if (target.exists() && !target.delete()) error("Unable to replace existing model")
        check(partial.renameTo(target)) { "Unable to finalize model file" }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
