package com.yahyaoui.prayermode

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

class AudioPlayerHelper(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val tag = "AudioPlayerHelper"

    suspend fun playAudioFromRaw(resourceId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.DEBUG) Log.d(tag, "Starting audio playback - resourceId: $resourceId")
            releaseMediaPlayer()

            return@withContext suspendCancellableCoroutine { continuation ->
                mediaPlayer = MediaPlayer.create(context, resourceId).apply {
                    if (this == null) {
                        continuation.resume(false)
                        return@apply
                    }
                    if (BuildConfig.DEBUG) Log.d(tag, "MediaPlayer created successfully")

                    setOnCompletionListener {
                        if (BuildConfig.DEBUG) Log.d(tag, "Audio playback completed successfully")
                        Handler(Looper.getMainLooper()).postDelayed({
                            releaseMediaPlayer()
                        }, 100)
                        continuation.resume(true)
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(tag, "Playback error: what=$what, extra=$extra")
                        releaseMediaPlayer()
                        continuation.resume(false)
                        true
                    }
                    start()
                }
            }
        } catch (e: IOException) {
            Log.e(tag, "IOException in playAudioFromRaw: ${e.message}", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(tag, "IllegalStateException in playAudioFromRaw: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error in playAudioFromRaw: ${e.message}", e)
            false
        }
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                if (BuildConfig.DEBUG) Log.d(tag, "Stopping current audio playback")
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
}