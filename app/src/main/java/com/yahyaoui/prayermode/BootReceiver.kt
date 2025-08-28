package com.yahyaoui.prayermode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    private val channelId = "location_switch_channel"
    private val notificationId = 1001
    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (BuildConfig.DEBUG) Log.d(tag, "Device rebooted. BootReceiver triggered.")

            val applicationContext = context.applicationContext
            val permissionsHelper = PermissionsHelper(applicationContext)
            val sharedHelper = SharedHelper(applicationContext)

            val isAppReadyToSchedule = sharedHelper.getSwitchState() &&
                    permissionsHelper.checkLocationPermission() &&
                    permissionsHelper.checkDNDPermission(applicationContext) &&
                    permissionsHelper.checkAlarmPermission() &&
                    permissionsHelper.checkBackgroundLocationPermission()

            if (isAppReadyToSchedule) {
                if (BuildConfig.DEBUG) Log.d(tag, "Switch is on, location, alarm and DND permissions are granted. Initiating work.")

                val dailyWorkRequest = OneTimeWorkRequestBuilder<SilentModeWorker>()
                    .setInputData(workDataOf("prayerName" to "DailyWorker"))
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .addTag("DailyPrayerScheduleBoot")
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        "BootInitWork",
                        ExistingWorkPolicy.REPLACE,
                        dailyWorkRequest
                    )
            } else {
                if (BuildConfig.DEBUG) Log.d(tag, "Switch is off or location, alarm and DND permissions are not granted.")
            }
            sendNotification(context)
        }
    }

    private fun sendNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Location Switch Channel"
            val descriptionText = "Notification to activate location switch after reboot"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TOGGLE_SWITCH_TWICE", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(context.getString(R.string.app_disabled_after_reboot))

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_prayer_mat_vector)
            .setContentTitle(context.getString(R.string.location_title))
            .setContentText(context.getString(R.string.app_disabled_after_reboot))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(bigTextStyle)
            .addAction(R.drawable.ic_prayer_mat_vector, context.getString(R.string.open_prayer), pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, builder.build())
                }
            } else Log.e(tag, "POST_NOTIFICATIONS permission not granted.")
        } else {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        }
    }
}