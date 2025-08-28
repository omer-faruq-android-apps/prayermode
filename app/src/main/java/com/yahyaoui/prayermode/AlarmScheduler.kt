package com.yahyaoui.prayermode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val dailyAlarmAction = "DAILY_WORKER_ALARM"
    private val dailyAlarmRequestCode = 3123
    private val tag = "AlarmScheduler"
    private val sharedHelper: SharedHelper by lazy { SharedHelper(context) }

    fun scheduleDailyAlarm() {
        if (alarmManager == null) {
            Log.e(tag, "Alarm service not available.")
            return
        }
        if (!sharedHelper.getSwitchState()) {
            if (BuildConfig.DEBUG) Log.d(tag, "Switch is off, canceling alarm")
            return
        }

        val intent = Intent(context, SilentModeReceiver::class.java).apply {
            action = dailyAlarmAction
        }
        val pendingIntent = PendingIntent.getBroadcast(context, dailyAlarmRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime.timeInMillis, pendingIntent)
            if (BuildConfig.DEBUG) Log.i(tag, "Alarm set for ${targetTime.timeInMillis} (${targetTime.time}) with action '$dailyAlarmAction'")
        } catch (e: SecurityException) {
            Log.e(tag, "SecurityException while setting alarm: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Error setting alarm: ${e.message}")
        }
    }

    fun cancelDailyAlarm() {
        if (alarmManager == null) {
            Log.e(tag, "Alarm service not available, cannot cancel.")
            return
        }

        val intent = Intent(context, SilentModeReceiver::class.java).apply {
            action = dailyAlarmAction
        }
        val pendingIntent = PendingIntent.getBroadcast(context, dailyAlarmRequestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        pendingIntent?.let {
            alarmManager.cancel(it)
            if (BuildConfig.DEBUG) Log.i(tag, "Daily alarm cancelled.")
        } ?: run {
            if (BuildConfig.DEBUG) Log.i(tag, "No daily alarm to cancel.")
        }
    }
}