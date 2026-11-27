package com.example.util

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object SmsScanScheduler {
    const val JOB_ID = 7301

    fun schedule(context: Context, intervalHours: Int) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        val clampedHours = intervalHours.coerceIn(1, 24)
        val intervalMs = clampedHours * 60L * 60L * 1000L

        val componentName = ComponentName(context, SmsPeriodicScanJobService::class.java)
        val jobInfo = JobInfo.Builder(JOB_ID, componentName)
            .setPeriodic(intervalMs)
            .setPersisted(true)
            .build()

        runCatching {
            scheduler.cancel(JOB_ID)
            scheduler.schedule(jobInfo)
        }
    }

    fun cancel(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        runCatching {
            scheduler.cancel(JOB_ID)
        }
    }

    fun syncSchedule(context: Context) {
        val prefs = Preferences(context.applicationContext)
        if (prefs.isPeriodicSmsScanEnabled) {
            schedule(context.applicationContext, prefs.periodicSmsScanIntervalHours)
        } else {
            cancel(context.applicationContext)
        }
    }
}
