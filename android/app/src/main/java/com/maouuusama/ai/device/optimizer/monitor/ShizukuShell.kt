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

            val remote = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val streamExecutor = Executors.newFixedThreadPool(2)
            val deadlineNanos = System.nanoTime() +
                TimeUnit.MILLISECONDS.toNanos(timeoutMs)

            try {
                val stdoutFuture = streamExecutor.submit<String> {
                    remote.inputStream.bufferedReader().use { it.readText() }
                }
                val stderrFuture = streamExecutor.submit<String> {
                    remote.errorStream.bufferedReader().use { it.readText() }
                }

                fun remainingMillis(): Long =
                    TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime())
                        .coerceAtLeast(1L)

                val output = stdoutFuture.get(remainingMillis(), TimeUnit.MILLISECONDS)
                val error = stderrFuture.get(remainingMillis(), TimeUnit.MILLISECONDS)

                val waitFuture = streamExecutor.submit<Int> {
                    remote.waitFor()
                }
                val exitCode = try {
                    waitFuture.get(remainingMillis(), TimeUnit.MILLISECONDS)
                } catch (timeout: java.util.concurrent.TimeoutException) {
                    remote.destroy()
                    waitFuture.cancel(true)
                    throw IllegalStateException(
                        "Shizuku shell command timed out after ${timeoutMs}ms"
                    )
                }

                if (exitCode != 0) {
                    val detail = error.trim().ifEmpty { "no stderr output" }
                    throw IllegalStateException(
                        "Shizuku command failed ($exitCode): $detail"
                    )
                }

                output
            } finally {
                if (remote.isAlive) remote.destroy()
                streamExecutor.shutdownNow()
            }
        }
    }
}
