package com.maouuusama.ai.device.optimizer.localai

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class QwenModelDownloader(private val context: Context) {
    companion object {
        private const val EXTRA_FREE_SPACE_BYTES = 128L * 1024L * 1024L
    }

    fun isInstalled(): Boolean {
        val file = QwenLocalModel.file(context)
        return file.isFile &&
            file.length() == QwenLocalModel.EXPECTED_SIZE_BYTES &&
            sha256(file) == QwenLocalModel.EXPECTED_SHA256
    }

    fun hasEnoughStorage(): Boolean {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBytes >= QwenLocalModel.EXPECTED_SIZE_BYTES + EXTRA_FREE_SPACE_BYTES
    }

    fun download(progress: (Long, Long) -> Unit = { _, _ -> }) {
        check(hasEnoughStorage()) { "Insufficient free storage for Qwen3 model" }

        val directory = QwenLocalModel.file(context).parentFile
            ?: error("Model directory unavailable")
        check(directory.mkdirs() || directory.isDirectory)

        val target = QwenLocalModel.file(context)
        val partial = QwenLocalModel.partialFile(context)
        var existing = if (partial.isFile) partial.length() else 0L

        if (existing > QwenLocalModel.EXPECTED_SIZE_BYTES) {
            check(partial.delete()) { "Unable to remove invalid partial model" }
            existing = 0L
        }

        val connection = (URL(QwenLocalModel.DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
            if (existing > 0L) setRequestProperty("Range", "bytes=${existing}-")
        }

        try {
            val code = connection.responseCode
            val append = existing > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (!append) existing = 0L
            if (code !in 200..299 && !append) {
                error("Model download failed with HTTP $code")
            }

            val total = when {
                append -> existing + connection.contentLengthLong.coerceAtLeast(0L)
                connection.contentLengthLong > 0L -> connection.contentLengthLong
                else -> QwenLocalModel.EXPECTED_SIZE_BYTES
            }

            connection.inputStream.use { input ->
                FileOutputStream(partial, append).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = existing
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        progress(downloaded, total)
                    }
                    output.fd.sync()
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
        if (target.exists() && !target.delete()) {
            error("Unable to replace existing model")
        }
        check(partial.renameTo(target)) {
            "Unable to finalize model file"
        }
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
