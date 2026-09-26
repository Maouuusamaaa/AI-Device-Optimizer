package com.maouuusama.ai.device.optimizer.benchmark

import android.app.job.JobParameters
import android.app.job.JobService

class FreshProcessPairedLifecycleJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        FreshProcessPairedLifecycleCoordinator.continueAfterFreshProcess(this) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
