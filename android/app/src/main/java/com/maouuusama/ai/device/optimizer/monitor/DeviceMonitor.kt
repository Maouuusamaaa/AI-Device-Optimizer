package com.maouuusama.ai.device.optimizer.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

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
            processes = processMonitor.collectProcesses(),
            systemTelemetry = if (includeSystemTelemetry) systemTelemetryMonitor.collectSnapshot() else null
        )
    }
}
