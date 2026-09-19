package com.maouuusama.ai.device.optimizer.monitor

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuShell {
    enum class Status { AVAILABLE, PERMISSION_REQUIRED, UNAVAILABLE }

    private const val USER_SERVICE_VERSION = 3
    private const val CONNECT_TIMEOUT_MS = 5_000L
    private const val DEFAULT_TIMEOUT_MS = 5_000L

    private val lock = Any()

    @Volatile private var remoteService: IShizukuShellService? = null
    private var binding = false
    private var connectionLatch = CountDownLatch(0)
    private var serviceArgs: Shizuku.UserServiceArgs? = null
    private var serviceConnection: ServiceConnection? = null

    fun status(): Status = try {
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) Status.UNAVAILABLE
        else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) Status.AVAILABLE
        else Status.PERMISSION_REQUIRED
    } catch (_: Throwable) {
        Status.UNAVAILABLE
    }

    fun requestPermission(requestCode: Int) {
        if (status() == Status.PERMISSION_REQUIRED) Shizuku.requestPermission(requestCode)
    }

    fun execute(
        context: Context,
        operation: ShizukuTelemetryOperation,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): Result<String> {
        if (timeoutMs !in 1L..60_000L) {
            return Result.failure(IllegalArgumentException("timeoutMs must be between 1 and 60000 ms"))
        }
        if (status() != Status.AVAILABLE) {
            return Result.failure(
                IllegalStateException("Shizuku is not available or permission is not granted")
            )
        }
        return runCatching {
            getRemoteService(context.applicationContext).execute(operation.name, timeoutMs)
        }
    }

    fun unbind() {
        synchronized(lock) {
            val args = serviceArgs
            val connection = serviceConnection
            remoteService = null
            binding = false
            connectionLatch.countDown()
            if (args != null && connection != null) {
                runCatching { Shizuku.unbindUserService(args, connection, true) }
            }
            serviceArgs = null
            serviceConnection = null
        }
    }

    private fun getRemoteService(context: Context): IShizukuShellService {
        val latch: CountDownLatch
        synchronized(lock) {
            remoteService?.let { service ->
                if (service.asBinder().pingBinder()) return service
                remoteService = null
            }

            if (!binding) {
                binding = true
                connectionLatch = CountDownLatch(1)
                val args = Shizuku.UserServiceArgs(
                    ComponentName(context, ShizukuShellUserService::class.java)
                )
                    .tag("shizuku_shell")
                    .version(USER_SERVICE_VERSION)
                    .daemon(true)
                    .processNameSuffix("shizuku_shell")
                    .debuggable(false)
                serviceArgs = args
                serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        synchronized(lock) {
                            remoteService = binder?.let(IShizukuShellService.Stub::asInterface)
                            binding = false
                            connectionLatch.countDown()
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        synchronized(lock) {
                            remoteService = null
                            binding = false
                            connectionLatch.countDown()
                        }
                    }
                }
                try {
                    Shizuku.bindUserService(args, serviceConnection!!)
                } catch (failure: Throwable) {
                    binding = false
                    connectionLatch.countDown()
                    throw failure
                }
            }
            latch = connectionLatch
        }

        if (!latch.await(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            synchronized(lock) { binding = false }
            throw IllegalStateException("Shizuku user service connection timed out")
        }
        synchronized(lock) {
            return remoteService
                ?: throw IllegalStateException("Shizuku user service did not provide a binder")
        }
    }
}
