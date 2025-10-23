package com.yahyaoui.prayermode

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val tag = "UpdateCheckWorker"

    override suspend fun doWork(): Result {
        if (BuildConfig.DEBUG) Log.d(tag, "Weekly update check started")
        val updateChecker = UpdateChecker(applicationContext)
        try {
            updateChecker.checkForUpdate()
            if (BuildConfig.DEBUG) Log.d(tag, "Weekly update check completed")
            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Error during weekly update check: ${e.message}")
            return Result.retry() // Or Result.failure() depending on your preference
        }
    }
}