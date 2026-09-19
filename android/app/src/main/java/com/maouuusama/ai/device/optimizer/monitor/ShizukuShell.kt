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

    private const val USER_SERVICE_VERSION = 2
    private const val CONNECT_TIMEOUT_MS = 5_000L

    private val lock = Any()

    @Volatile
    private var remoteService: IShizukuShellService? = null

    private var binding = false
    private var connectionLatch = CountDownLatch(0)
    private var serviceArgs: Shizuku.UserServiceArgs? = null
    private var serviceConnection: ServiceConnection? = null

    fun status(): Status = try {
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
            Status.UNAVAILABLE
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Status.AVAILABLE
        } else {
            Status.PERMISSION_REQUIRED
        }
    } catch (_: Throwable) {
        Status.UNAVAILABLE
    }

    fun requestPermission(requestCode: Int) {
        if (status() == Status.PERMISSION_REQUIRED) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun execute(
        context: Context,
        command: String,
        timeoutMs: Long = 5_000L
    ): Result<String> {
        if (status() != Status.AVAILABLE) {
            return Result.failure(
                IllegalStateException("Shizuku is not available or permission is not granted")
            )
        }

        return runCatching {
            val service = getRemoteService(context.applicationContext)
            service.execute(command, timeoutMs)
        }
    }

    private fun getRemoteService(context: Context): IShizukuShellService {
        synchronized(lock) {
            remoteService?.let { service ->
                if (service.asBinder().pingBinder()) {
                    return service
                }
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
                        }
                    }
                }

                try {
                    Shizuku.bindUserService(args, serviceConnection!!)
                } catch (throwable: Throwable) {
                    binding = false
                    connectionLatch.countDown()
                    throw throwable
                }
            }

            val latch = connectionLatch
            if (!latch.await(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                binding = false
                throw IllegalStateException("Shizuku user service connection timed out")
            }

            return remoteService
                ?: throw IllegalStateException("Shizuku user service did not provide a binder")
        }
    }
}
