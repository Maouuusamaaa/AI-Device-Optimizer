package com.maouuusama.ai.device.optimizer.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock

class DeviceMonitor(private val context: Context) {

    private val processMonitor = ProcessMonitor(context)
    private val systemTelemetryMonitor = SystemTelemetryMonitor()

    fun collectSnapshot(includeSystemTelemetry: Boolean = true): DeviceSnapshot {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryPercent = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            ?.takeIf { it in 0..100 }
        val status = battery?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val batteryTemperatureC = battery?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            Int.MIN_VALUE
        )?.takeIf { it != Int.MIN_VALUE }?.let { it / 10.0 }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            null
        }

        val statFs = StatFs(Environment.getDataDirectory().path)
        val storageTotalBytes = statFs.totalBytes
        val storageFreeBytes = statFs.availableBytes

        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        val capabilities = network?.let { connectivity.getNetworkCapabilities(it) }
        val networkTransport = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ETHERNET"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true -> "BLUETOOTH"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
            capabilities != null -> "OTHER"
            else -> null
        }
        val networkValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return DeviceSnapshot(
            timestampMs = System.currentTimeMillis(),
            androidApi = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            totalRamMb = memoryInfo.totalMem / 1024 / 1024,
            availableRamMb = memoryInfo.availMem / 1024 / 1024,
            batteryPercent = batteryPercent,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            batteryTemperatureC = batteryTemperatureC,
            thermalStatus = thermalStatus,
            storageTotalBytes = storageTotalBytes,
            storageFreeBytes = storageFreeBytes,
            networkTransport = networkTransport,
            networkValidated = networkValidated,
            isInteractive = powerManager.isInteractive,
            uptimeMs = SystemClock.elapsedRealtime(),
            processes = processMonitor.collectProcesses(),
            systemTelemetry = if (includeSystemTelemetry) systemTelemetryMonitor.collectSnapshot() else null
        )
    }
}
