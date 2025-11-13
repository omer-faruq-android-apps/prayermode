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
        scheduleDailyUpdateCheck()
    }
    private fun scheduleDailyUpdateCheck() {
        val updateRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            1, TimeUnit.DAYS,
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "dailyUpdateCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )

        if (BuildConfig.DEBUG) Log.d(tag, "Daily update check scheduled")
    }
}