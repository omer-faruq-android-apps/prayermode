package com.yahyaoui.prayermode

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val tag = "AudioPlayerHelper"
    private val lock = Any()

    suspend fun playAudioFromRaw(resourceId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.DEBUG) Log.d(tag, "Starting audio playback - resourceId: $resourceId")
            releaseMediaPlayer()

            return@withContext suspendCancellableCoroutine { continuation ->
                val player = try {
                    MediaPlayer.create(context, resourceId)
                } catch (e: Exception) {
                    Log.e(tag, "Error creating MediaPlayer: ${e.message}", e)
                    if (continuation.context.isActive) continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                if (player == null) {
                    Log.e(tag, "Failed to create MediaPlayer (null result)")
                    if (continuation.context.isActive) continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                synchronized(lock) {
                    mediaPlayer = player
                }

                player.apply {
                    if (BuildConfig.DEBUG) Log.d(tag, "MediaPlayer created successfully")

                    setOnCompletionListener {
                        if (BuildConfig.DEBUG) Log.d(tag, "Audio playback completed")
                        releaseMediaPlayer()
                        if (continuation.context.isActive) continuation.resume(true)
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e(tag, "Playback error: what=$what, extra=$extra")
                        releaseMediaPlayer()
                        if (continuation.context.isActive) continuation.resume(false)
                        true
                    }

                    start()
                }

                continuation.invokeOnCancellation {
                    if (BuildConfig.DEBUG) Log.d(tag, "Coroutine cancelled")
                    releaseMediaPlayer()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in playAudioFromRaw: ${e.message}", e)
            false
        }
    }

    fun releaseMediaPlayer() {
        synchronized(lock) {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        if (BuildConfig.DEBUG) Log.d(tag, "Stopping playback")
                        it.stop()
                    }
                    it.release()
                    if (BuildConfig.DEBUG) Log.d(tag, "MediaPlayer released")
                } catch (e: Exception) {
                    Log.e(tag, "Error releasing MediaPlayer: ${e.message}", e)
                }
            }
            mediaPlayer = null
        }
    }
}