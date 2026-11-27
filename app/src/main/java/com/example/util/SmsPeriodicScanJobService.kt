package com.example.util

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsPeriodicScanJobService : JobService() {

    private var scanJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        val appContext = applicationContext
        val prefs = Preferences(appContext)
        if (!prefs.isPeriodicSmsScanEnabled) {
            jobFinished(params, false)
            return false
        }

        scanJob?.cancel()
        scanJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val unregistered = BackgroundSmsScanner.scanForUnregisteredSms(
                    context = appContext,
                    days = 3
                )
                if (unregistered.isNotEmpty()) {
                    val alreadyNotified = prefs.notifiedSmsHashes
                    val freshItems = unregistered.filter { it.smsHash !in alreadyNotified }
                    if (freshItems.isNotEmpty()) {
                        SmsScanNotificationHelper.showNewTransactionsNotification(appContext, freshItems)
                        prefs.notifiedSmsHashes = alreadyNotified + freshItems.map { it.smsHash }
                    }
                }
            } catch (_: Throwable) {
                // Swallow background scan errors cleanly
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        scanJob?.cancel()
        scanJob = null
        return true
    }
}
