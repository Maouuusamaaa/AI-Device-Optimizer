package com.maouuusama.ai.device.optimizer.sync

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class GitHubHttpException(val statusCode: Int, message: String) : Exception(message)

sealed class RemoteEvidence {
    data class Existing(val bytes: ByteArray, val sha: String) : RemoteEvidence()
    data object Missing : RemoteEvidence()
}

data class UploadResult(val commitSha: String?)

class GitHubEvidenceClient(
    private val token: String,
    private val repository: String,
    private val branch: String
) {
    init {
        require(repository.matches(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$"))) { "Repository must be owner/name" }
        require(branch.matches(Regex("^[A-Za-z0-9._/-]+$"))) { "Invalid branch name" }
    }

    fun verifyRepository() {
        val response = request("GET", "/repos/$repository")
        if (response.code !in 200..299) {
            throw GitHubHttpException(response.code, "Repository verification failed (${response.code})")
        }
    }

    fun uploadIfMissingOrIdentical(item: EvidenceQueueItem, file: File): UploadResult {
        when (val remote = getContent(item.repositoryPath)) {
            is RemoteEvidence.Existing -> {
                if (sha256(remote.bytes) == item.sha256) return UploadResult(null)
                throw GitHubHttpException(409, "Remote evidence path already exists with different content")
            }
            RemoteEvidence.Missing -> Unit
        }

        val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val payload = JSONObject()
            .put("message", "benchmarks: add ${file.name}")
            .put("content", encoded)
            .put("branch", branch)
        val response = request(
            "PUT",
            "/repos/$repository/contents/${encodePath(item.repositoryPath)}",
            payload.toString()
        )
        if (response.code !in 200..299) {
            throw GitHubHttpException(
                response.code,
                "Evidence upload failed (${response.code}): ${response.body.take(250)}"
            )
        }
        val root = JSONObject(response.body)
        return UploadResult(root.optJSONObject("commit")?.optString("sha")?.ifBlank { null })
    }

    private fun getContent(path: String): RemoteEvidence {
        val response = request(
            "GET",
            "/repos/$repository/contents/${encodePath(path)}?ref=${encodeQuery(branch)}"
        )
        return when {
            response.code == HttpURLConnection.HTTP_NOT_FOUND -> RemoteEvidence.Missing
            response.code in 200..299 -> {
                val root = JSONObject(response.body)
                val encoded = root.optString("content", "").replace("\n", "").replace("\r", "")
                require(encoded.isNotBlank()) { "GitHub returned empty content for existing evidence" }
                RemoteEvidence.Existing(Base64.decode(encoded, Base64.DEFAULT), root.optString("sha"))
            }
            else -> throw GitHubHttpException(
                response.code,
                "GitHub content lookup failed (${response.code}): ${response.body.take(250)}"
            )
        }
    }

    private data class Response(val code: Int, val body: String)

    private fun request(method: String, path: String, body: String? = null): Response {
        val connection = (URL(API_BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", API_VERSION)
            setRequestProperty("User-Agent", "AI-Device-Optimizer-EvidenceSync")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
            Response(code, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun encodePath(path: String): String = path.split('/').joinToString("/") { encodeQuery(it) }
    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        private const val API_BASE = "https://api.github.com"
        private const val API_VERSION = "2026-03-10"
    }
}
