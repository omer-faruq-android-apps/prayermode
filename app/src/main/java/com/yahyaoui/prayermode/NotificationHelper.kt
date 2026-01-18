package com.yahyaoui.prayermode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "prayer_channel"

    fun sendNotification(context: Context, titleResId: Int, messageResId: Int, notificationId: Int, vararg formatArgs: Any) {
        val sharedHelper = SharedHelper(context)
        if (!sharedHelper.getNotificationSwitchState()) {
            return
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val title = context.getString(titleResId)
        val message = context.getString(messageResId, *formatArgs)
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(message)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_mat_vector)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(bigTextStyle)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}