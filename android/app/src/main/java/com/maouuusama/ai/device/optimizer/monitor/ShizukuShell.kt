package com.maouuusama.ai.device.optimizer.monitor

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ShizukuShell {
    enum class Status { AVAILABLE, PERMISSION_REQUIRED, UNAVAILABLE }

    fun status(): Status = try {
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) Status.UNAVAILABLE
        else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) Status.AVAILABLE
        else Status.PERMISSION_REQUIRED
    } catch (_: Throwable) {
        Status.UNAVAILABLE
    }

    fun requestPermission(requestCode: Int) {
        if (status() == Status.PERMISSION_REQUIRED) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun execute(command: String, timeoutMs: Long = 5_000L): Result<String> {
        if (status() != Status.AVAILABLE) {
            return Result.failure(
                IllegalStateException("Shizuku is not available or permission is not granted")
            )
        }

        return runCatching {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val remote = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val executor = Executors.newFixedThreadPool(2)
            try {
                // Shizuku's RemoteProcess uses IPC-backed streams. Read stdout and
                // stderr concurrently so one full pipe cannot block process exit.
                val stdoutFuture = executor.submit<String> {
                    remote.inputStream.bufferedReader().use { it.readText() }
                }
                val stderrFuture = executor.submit<String> {
                    remote.errorStream.bufferedReader().use { it.readText() }
                }

                if (!remote.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                    remote.destroy()
                    throw IllegalStateException("Shizuku shell command timed out")
                }

                val output = stdoutFuture.get()
                val error = stderrFuture.get()
                val exitCode = remote.exitValue()

                if (exitCode != 0) {
                    throw IllegalStateException(
                        "Shizuku command failed ($exitCode): ${error.trim()}"
                    )
                }

                output
            } finally {
                if (remote.isAlive) remote.destroy()
                executor.shutdownNow()
            }
        }
    }
}
