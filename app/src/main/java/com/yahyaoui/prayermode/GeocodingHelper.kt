package com.yahyaoui.prayermode

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

object GeocodingHelper {

    private fun Address.getLocationName(): String? {
        return locality ?: adminArea ?: countryName
    }

    suspend fun getLocationName(context: Context, location: Location?): String? {
        if (location == null) return null
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            continuation.resume(addresses.firstOrNull()?.getLocationName())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.getLocationName()
                }
            } catch (e: IOException) {
                Log.e("GeocodingHelper", "Network error: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("GeocodingHelper", "Invalid coordinates: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e("GeocodingHelper", "Unexpected error: ${e.message}")
                null
            }
        }
    }
}