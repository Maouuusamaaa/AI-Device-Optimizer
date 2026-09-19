package com.maouuusama.ai.device.optimizer.monitor

import android.os.RemoteException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShizukuShellUserService : IShizukuShellService.Stub() {

    private val ioExecutor = Executors.newCachedThreadPool()

    override fun execute(command: String?, timeoutMs: Long): String {
        require(!command.isNullOrBlank()) { "command must not be blank" }
        require(timeoutMs > 0) { "timeoutMs must be positive" }

        val process = try {
            ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()
        } catch (error: Exception) {
            throw RemoteException("Unable to start shell: ${error.message}")
        }

        val stdoutFuture = ioExecutor.submit<String> {
            process.inputStream.bufferedReader().use { it.readText() }
        }
        val stderrFuture = ioExecutor.submit<String> {
            process.errorStream.bufferedReader().use { it.readText() }
        }

        try {
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroy()
                process.waitFor(250, TimeUnit.MILLISECONDS)
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                stdoutFuture.cancel(true)
                stderrFuture.cancel(true)
                throw RemoteException("Shizuku shell command timed out")
            }

            val output = stdoutFuture.get(1, TimeUnit.SECONDS)
            val error = stderrFuture.get(1, TimeUnit.SECONDS)
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                throw RemoteException(
                    "Shizuku command failed ($exitCode): ${error.trim()}"
                )
            }

            return output
        } catch (error: RemoteException) {
            throw error
        } catch (error: Exception) {
            throw RemoteException(
                "Shizuku user service execution failed: ${error.message}"
            )
        } finally {
            if (process.isAlive) {
                process.destroy()
            }
        }
    }

    override fun exit() {
        destroy()
    }

    override fun destroy() {
        ioExecutor.shutdownNow()
        System.exit(0)
    }
}
