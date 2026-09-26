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

        // There are two distinct continuation states:
        // 1. An active pair has KEY_PAIR_ID/RESET_FILE and must resume its
        //    no-reset control arm.
        // 2. A completed pair has KEY_NEXT_PAIR_PENDING and must start the
        //    next independent reset-enabled pair.
        //
        // The second state is exactly what is needed after pair 1. Do not
        // rely on START_REDELIVER_INTENT here: an explicit Process.killProcess()
        // is not a system service restart request, so Android is not required
        // to recreate the foreground service.
        if (FreshProcessPairedLifecycleCoordinator.startPendingNextPair(this)) {
            jobFinished(params, false)
            return true
        }

        FreshProcessPairedLifecycleCoordinator.continueAfterFreshProcess(this) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
