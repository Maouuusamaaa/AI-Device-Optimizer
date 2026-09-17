package com.maouuusama.ai.device.optimizer.monitor

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build

class DeviceMonitor(private val context: Context) {

    fun collectSnapshot(): DeviceSnapshot {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val batteryPercent = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .coerceIn(0, 100)

        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

        return DeviceSnapshot(
            timestampMs = System.currentTimeMillis(),
            androidApi = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            totalRamMb = memoryInfo.totalMem / 1024 / 1024,
            availableRamMb = memoryInfo.availMem / 1024 / 1024,
            batteryPercent = batteryPercent,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        )
    }
}
