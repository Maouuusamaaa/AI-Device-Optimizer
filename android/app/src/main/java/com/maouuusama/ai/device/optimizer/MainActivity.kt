package com.maouuusama.ai.device.optimizer

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.maouuusama.ai.device.optimizer.monitor.DeviceMonitor

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val snapshot = DeviceMonitor(this).collectSnapshot()
        val text = "AI Device Optimizer\n" +
            "Monitor prototype\n\n" +
            "Android API: " + snapshot.androidApi + "\n" +
            "Device: " + snapshot.manufacturer + " " + snapshot.model + "\n" +
            "RAM total: " + snapshot.totalRamMb + " MB\n" +
            "RAM available: " + snapshot.availableRamMb + " MB\n" +
            "Battery: " + snapshot.batteryPercent + "%\n" +
            "Charging: " + snapshot.isCharging

        setContentView(TextView(this).apply {
            this.text = text
            textSize = 18f
            setPadding(32, 48, 32, 32)
        })
    }
}
