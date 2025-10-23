package com.yahyaoui.prayermode

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MyApp : Application() {
    private val tag = "MyApp"

    override fun onCreate() {
        super.onCreate()
        scheduleWeeklyUpdateCheck()
    }
    private fun scheduleWeeklyUpdateCheck() {
        val updateRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            7, TimeUnit.DAYS, // Repeat every 7 days
            1, TimeUnit.HOURS // Flexible interval of 1 hour
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weeklyUpdateCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )

        if (BuildConfig.DEBUG) Log.d(tag, "Weekly update check scheduled")
    }
}