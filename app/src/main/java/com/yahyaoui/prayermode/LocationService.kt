package com.yahyaoui.prayermode

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class LocationService : Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private val channelId = "location_service_channel"
    private val tools: Tools by lazy { Tools(applicationContext) }
    private val sharedHelper: SharedHelper by lazy { SharedHelper(applicationContext) }
    private val locationMutex = Mutex()
    private val tag = "LocationService"
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    companion object {
        const val PREF_LAST_FETCH_LATITUDE = "last_fetch_latitude"
        const val PREF_LAST_FETCH_LONGITUDE = "last_fetch_longitude"
        const val PREF_LAST_FETCH_TIME_MS = "last_fetch_time_ms"
        private const val SIGNIFICANT_DISPLACEMENT_KM = 25f
        private const val MAX_LOCATION_AGE_MS = 30 * 60000L
        private const val MIN_LOCATION_ACCURACY_METERS = 100f
        private const val LOCATION_UPDATE_INTERVAL_MS = 20 * 60000L
        private const val LOCATION_UPDATE_DISTANCE_METERS = 250f
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        startForegroundService()
        initLocationListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) Log.d(tag, "Service started")
        if (!isLocationEnabled()) {
            NotificationHelper.sendNotification(this@LocationService, R.string.location_title, R.string.location_service_disabled, 313, "")
            stopSelf()
        } else requestLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationListener.isInitialized) locationManager.removeUpdates(locationListener)
        coroutineContext.cancel()
        if (BuildConfig.DEBUG) Log.d(tag, "Location updates stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val sharedHelper = SharedHelper(this)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.location_title))
            .setContentText(getString(R.string.getting_location_update))
            .setSmallIcon(R.drawable.ic_prayer_mat_vector)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        
        if (!sharedHelper.getNotificationSwitchState()) {
            builder.setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        }
        
        startForeground(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notification channel for location service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun initLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (BuildConfig.DEBUG) Log.d(tag, "Location update received: Latitude=${location.latitude}, Longitude=${location.longitude}, Accuracy=${location.accuracy}")
                handleLocationChange(location)
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {
                if (BuildConfig.DEBUG) Log.d(tag, "Provider enabled: $provider")
            }
            override fun onProviderDisabled(provider: String) {
                if (BuildConfig.DEBUG) Log.d(tag, "Provider disabled: $provider")
            }
        }
    }

    private fun handleLocationChange(newLocation: Location) {
        launch {
            locationMutex.withLock {
                val locationAge = System.currentTimeMillis() - newLocation.time
                if (locationAge > MAX_LOCATION_AGE_MS || !isLocationAccurate(newLocation)) {
                    if (BuildConfig.DEBUG) Log.w(tag, "Rejecting stale/inaccurate location...")
                    return@withLock
                }
                val lastFetchLatitude = sharedHelper.getDouble(PREF_LAST_FETCH_LATITUDE, Double.NaN)
                val lastFetchLongitude = sharedHelper.getDouble(PREF_LAST_FETCH_LONGITUDE, Double.NaN)
                val lastFetchTimeMs = sharedHelper.getLong(PREF_LAST_FETCH_TIME_MS, 0L)

                if (lastFetchLatitude.isNaN() || lastFetchLongitude.isNaN() || lastFetchTimeMs == 0L) {
                    if (BuildConfig.DEBUG) Log.i(tag, "Initial state or invalid data detected.")
                    return@withLock
                }

                val lastFetchLocation = Location("lastFetch").apply {
                    latitude = lastFetchLatitude
                    longitude = lastFetchLongitude
                }

                val displacementKm = lastFetchLocation.distanceTo(newLocation).div(1000)

                if (BuildConfig.DEBUG) {
                    val locationAge = System.currentTimeMillis() - newLocation.time
                    Log.d(tag, "--- Location Update Check ---")
                    Log.d(tag, "New Location: Lat=${"%.4f".format(newLocation.latitude)}, Lon=${"%.4f".format(newLocation.longitude)}, " + "Acc=${"%.1f".format(newLocation.accuracy)}m, Age=${(locationAge / 1000).toInt()}s")
                    Log.d(tag, "Last Fetch Data from SharedPreferences: Lat: ${"%.4f".format(lastFetchLatitude)}, Lon: ${"%.4f".format(lastFetchLongitude)}")
                    Log.d(tag, "- Displacement from last fetch: ${"%.1f".format(displacementKm)} km")
                    Log.d(tag, "--- End of Check ---")
                }

                if (displacementKm >= SIGNIFICANT_DISPLACEMENT_KM && newLocation.accuracy <= MIN_LOCATION_ACCURACY_METERS) {
                    if (BuildConfig.DEBUG) Log.i(tag, "Significant displacement with good accuracy. Fetching prayer times.")
                    triggerPrayerTimesFetch(newLocation, displacementKm)
                } else if (displacementKm >= SIGNIFICANT_DISPLACEMENT_KM) {
                    if (BuildConfig.DEBUG) Log.w(tag, "Significant displacement detected but accuracy is poor. Waiting for better fix.")
                }
            }
        }
    }

    private fun triggerPrayerTimesFetch(currentLocation: Location, distance: Float) {
        if (!sharedHelper.getSwitchState()) {
            if (BuildConfig.DEBUG) Log.i(tag, "Skipping fetch : main switch is off.")
            return
        }

        val methodIndex = sharedHelper.getIntValue(SharedHelper.SELECTED_METHOD_RES_ID, 0)
        val method = sharedHelper.getStringFromArray(R.array.calculation_methods, SharedHelper.SELECTED_METHOD_RES_ID, 0)
        if (BuildConfig.DEBUG) Log.i(tag, "Triggering fetch, method Id: $method, displacement: ${"%.1f".format(distance)} km")

        launch {
            try {
                withContext(Dispatchers.IO) {
                    if (tools.findLocation(methodIndex)) {
                        if (BuildConfig.DEBUG) Log.i(tag, "Prayer times fetch successful due to travel.")
                        if (distance>0) NotificationHelper.sendNotification(this@LocationService, R.string.location_title, R.string.travelled_distance, 333, "%.1f".format(distance))
                        tools.exitSilentMode()
                        tools.cancelAllSilentModes()
                        tools.cancelScheduledSilentMode()
                        AlarmScheduler(applicationContext).scheduleDailyAlarm()
                        sharedHelper.saveDouble(PREF_LAST_FETCH_LATITUDE, currentLocation.latitude)
                        sharedHelper.saveDouble(PREF_LAST_FETCH_LONGITUDE, currentLocation.longitude)
                    } else Log.e(tag, "Prayer times fetch failed")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching prayer times: ${e.message}",e)
            }
        }
    }

    private fun requestLocationUpdates() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            stopSelf()
            return
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasFineLocation) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                LOCATION_UPDATE_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                LOCATION_UPDATE_DISTANCE_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun isLocationAccurate(location: Location): Boolean {
        return location.accuracy <= MIN_LOCATION_ACCURACY_METERS
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}