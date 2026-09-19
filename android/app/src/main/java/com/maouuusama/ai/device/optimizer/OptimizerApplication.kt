package com.maouuusama.ai.device.optimizer

import android.app.ActivityManager
import android.app.Application
import android.os.Build

class OptimizerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        recordPreviousExitReason()
    }

    private fun recordPreviousExitReason() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val activityManager = getSystemService(ActivityManager::class.java)
        val exit = activityManager.getHistoricalProcessExitReasons(packageName, 0, 1)
            .firstOrNull() ?: return

        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putInt("last_exit_reason", exit.reason)
            .putString("last_exit_description", exit.description)
            .putLong("last_exit_timestamp_ms", exit.timestamp)
            .apply()
    }

    companion object {
        private const val PREFERENCES = "optimizer_agent"
    }
}
