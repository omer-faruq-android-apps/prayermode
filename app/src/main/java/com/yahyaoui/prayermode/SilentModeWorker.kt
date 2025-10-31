package com.yahyaoui.prayermode

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SilentModeWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val sharedHelper: SharedHelper by lazy { SharedHelper(appContext) }
    private val tools: Tools by lazy { Tools(appContext) }
    private val tag = "SilentModeWorker"
    private val audioPlayerHelper: AudioPlayerHelper by lazy { AudioPlayerHelper(appContext) }


    override suspend fun doWork(): Result {
        val mode = inputData.getBoolean("mode", false)
        val prayerName = inputData.getString("prayerName") ?: "unknown"
        if (BuildConfig.DEBUG) Log.d(tag, "Executing worker for $prayerName: mode=$mode")

        return try {
            withContext(Dispatchers.IO) {
                if (!sharedHelper.getSwitchState()) {
                    if (BuildConfig.DEBUG) Log.d(tag, "Switch is off, canceling worker")
                    return@withContext Result.success()
                }

                if (prayerName == "DailyWorker") {
                    val calendar = Calendar.getInstance()
                    val gregorianDay = calendar.get(Calendar.DAY_OF_MONTH)
                    val monthFormat = SimpleDateFormat("MMMM", Locale.US)
                    val gregorianMonthName = monthFormat.format(calendar.time)
                    val selectedMethodIndex = sharedHelper.getIntValue(SharedHelper.SELECTED_METHOD_RES_ID, 0)
                    if (BuildConfig.DEBUG) Log.i(tag, "Daily worker processing, gregorian day is $gregorianDay")

                     if (gregorianDay == 1) {
                        if (selectedMethodIndex != -1) {
                            if (BuildConfig.DEBUG) {
                                Log.i(tag, "Fetching for $gregorianMonthName prayer times, Method Index is $selectedMethodIndex")
                                NotificationHelper.sendNotification(applicationContext, R.string.fetch_title, R.string.monthly_update, 200, gregorianMonthName)
                            }
                            tools.findLocation(selectedMethodIndex)
                            AlarmScheduler(applicationContext).scheduleDailyAlarm()
                        } else {
                            if (BuildConfig.DEBUG) {
                                Log.i(tag, "Method index is not set. Using default method.")
                                NotificationHelper.sendNotification(applicationContext, R.string.fetch_title, R.string.monthly_update_default_method, 200, gregorianMonthName)
                            }
                            tools.findLocation(4)
                            AlarmScheduler(applicationContext).scheduleDailyAlarm()
                        }
                    } else {
                        tools.processPrayerTimes()
                        AlarmScheduler(applicationContext).scheduleDailyAlarm()
                    }
                    return@withContext Result.success()
                } else {
                    val silentModeSetSuccess = if (mode) {
                        if (sharedHelper.getAudioSwitchState() && sharedHelper.getSwitchState() && prayerName != "Joumoua") {
                            if (BuildConfig.DEBUG) Log.i(tag, "Audio switch and main switch are on, not Joumoua prayer, playing audio...")
                            audioPlayerHelper.playAudioFromRaw(R.raw.takbir)
                        }
                        if (BuildConfig.DEBUG) Log.i(tag, "Activating silent mode for $prayerName.")
                        tools.setSilentMode(true, prayerName)
                    } else {
                        if (BuildConfig.DEBUG) Log.i(tag, "Deactivating silent mode for $prayerName.")
                        tools.setSilentMode(false, prayerName)
                    }

                    if (!silentModeSetSuccess) {
                        Log.e(tag, "Failed to set silent mode for $prayerName (likely DND permission missing or other issue). Worker failing.")
                        return@withContext Result.failure()
                    }
                    return@withContext Result.success()
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) NotificationHelper.sendNotification(applicationContext, R.string.worker_failed, R.string.worker_failed, 222, "${e.message}")
            Log.e(tag, "Worker failed: ${e.message}.", e)
            return Result.failure()
        }
    }
}