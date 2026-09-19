package com.maouuusama.ai.device.optimizer.monitor

import android.os.RemoteException
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShizukuShellUserService : IShizukuShellService.Stub() {

    companion object {
        private const val MAX_OUTPUT_CHARS = 128 * 1024
        private const val STREAM_GRACE_MS = 1_000L
        private const val DESTROY_GRACE_MS = 250L
    }

    private val ioExecutor = Executors.newFixedThreadPool(2)

    override fun execute(operation: String?, timeoutMs: Long): String {
        require(!operation.isNullOrBlank()) { "operation must not be blank" }
        require(timeoutMs in 1L..60_000L) { "timeoutMs must be between 1 and 60000 ms" }

        val telemetryOperation = runCatching {
            ShizukuTelemetryOperation.valueOf(operation)
        }.getOrElse {
            throw RemoteException("Unsupported Shizuku telemetry operation")
        }
        val command = ShizukuTelemetryCommands.commandFor(telemetryOperation)

        val process = try {
            ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()
        } catch (failure: Exception) {
            throw RemoteException("Unable to start telemetry command: " + failure.message)
        }

        val stdoutFuture = ioExecutor.submit<String> { readLimited(process.inputStream) }
        val stderrFuture = ioExecutor.submit<String> { readLimited(process.errorStream) }

        try {
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                terminate(process)
                stdoutFuture.cancel(true)
                stderrFuture.cancel(true)
                throw RemoteException("Shizuku telemetry operation timed out")
            }

            val output = stdoutFuture.get(STREAM_GRACE_MS, TimeUnit.MILLISECONDS)
            val error = stderrFuture.get(STREAM_GRACE_MS, TimeUnit.MILLISECONDS)
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                throw RemoteException(
                    "Shizuku telemetry operation failed (" + exitCode + "): " +
                        error.take(2_000).trim()
                )
            }
            return output
        } catch (failure: RemoteException) {
            throw failure
        } catch (failure: Exception) {
            terminate(process)
            throw RemoteException(
                "Shizuku telemetry execution failed: " + failure.message
            )
        } finally {
            if (process.isAlive) terminate(process)
        }
    }

    override fun destroy() {
        ioExecutor.shutdownNow()
        System.exit(0)
    }

    private fun readLimited(stream: InputStream): String {
        BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
            val builder = StringBuilder()
            val buffer = CharArray(8 * 1024)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                if (builder.length + count > MAX_OUTPUT_CHARS) {
                    throw RemoteException("Telemetry output exceeded safety limit")
                }
                builder.append(buffer, 0, count)
            }
            return builder.toString()
        }
    }

    private fun terminate(process: Process) {
        process.destroy()
        runCatching { process.waitFor(DESTROY_GRACE_MS, TimeUnit.MILLISECONDS) }
        if (process.isAlive) process.destroyForcibly()
    }
}
