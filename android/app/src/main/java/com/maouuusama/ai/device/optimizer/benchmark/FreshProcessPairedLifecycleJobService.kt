package com.maouuusama.ai.device.optimizer.benchmark

import android.app.job.JobScheduler
import android.app.job.JobParameters
import android.app.job.JobService

class FreshProcessPairedLifecycleJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        FreshProcessPairedLifecycleCoordinator.notifyStatus(
            this,
            "Fresh-process continuation triggered",
            "The system resumed the paired lifecycle after the original process ended."
        )
        getSystemService(JobScheduler::class.java)?.cancel(0xA1D1)
        // The foreground optimizer service owns the active pair execution. Its
        // START_REDELIVER_INTENT restart is the continuation mechanism after
        // the intentional process boundary. This JobService remains only as
        // a persisted-state fallback/observer and must not launch a second
        // pair concurrently with the redelivered service intent.
        FreshProcessPairedLifecycleCoordinator.continueAfterFreshProcess(this) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
