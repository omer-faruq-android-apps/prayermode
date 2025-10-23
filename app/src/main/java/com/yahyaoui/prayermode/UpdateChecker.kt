package com.yahyaoui.prayermode

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import androidx.core.net.toUri
import android.app.*
import android.os.Build
import androidx.core.app.NotificationCompat

data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val minRequired: Boolean = false
)

object NotificationIds {
    const val UPDATE_AVAILABLE = 1
}

class UpdateChecker(private val context: Context) {

    private companion object {
        const val VERSION_JSON_URL = "https://prayermode.github.io/apk/version.json"
        const val UPDATE_CHANNEL_ID = "update_channel"
    }

    private val tag = "UpdateChecker"

    private suspend fun getLatestVersion(): AppVersion? {
        return try {
            val jsonString = withContext(Dispatchers.IO) {
                URL(VERSION_JSON_URL).readText()
            }
            val jsonObject = JSONObject(jsonString)
            AppVersion(
                versionCode = jsonObject.getInt("versionCode"),
                versionName = jsonObject.getString("versionName"),
                apkUrl = jsonObject.getString("apkUrl"),
                minRequired = jsonObject.optBoolean("minRequired", false)
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching version: ${e.message}")
            null
        }
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)).toInt()
        } catch (e: Exception) {
            Log.e(tag, "Error getting current version: ${e.message}")
            -1
        }
    }

    suspend fun checkForUpdate() {
        if (BuildConfig.DEBUG) Log.d(tag, "Checking for updates...")
        val latestVersion = getLatestVersion() ?: return
        val currentVersion = getCurrentVersionCode()
        if (BuildConfig.DEBUG) Log.d(tag, "Current: $currentVersion, Latest: ${latestVersion.versionCode}")
        if (latestVersion.versionCode > currentVersion) {
            if (BuildConfig.DEBUG) Log.i(tag, "New version found: ${latestVersion.versionName}")
            showUpdateNotification(latestVersion)
        } else {
            if (BuildConfig.DEBUG) Log.d(tag, "Already on latest version")
        }
    }

    private fun showUpdateNotification(version: AppVersion) {
        createNotificationChannel()

        val title = context.getString(R.string.update_notification_title)
        val message = context.getString(R.string.update_notification_message, version.versionName)

        val updateIntent = Intent(Intent.ACTION_VIEW, version.apkUrl.toUri())
        val updatePendingIntent = PendingIntent.getActivity(
            context,
            1,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_mat_vector)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .addAction(0, context.getString(R.string.update), updatePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationIds.UPDATE_AVAILABLE, notification)

        if (BuildConfig.DEBUG) Log.d("UpdateChecker", "Update notification sent for version ${version.versionName}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new app updates"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}