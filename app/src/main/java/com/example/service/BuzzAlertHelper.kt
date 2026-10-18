package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object BuzzAlertHelper {

    private var activeMediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive = _isAlarmActive.asStateFlow()

    private val _alarmLocationTitle = MutableStateFlow("")
    val alarmLocationTitle = _alarmLocationTitle.asStateFlow()

    /**
     * Starts continuous full volume buzzing sound and repeating vibration
     * that will NOT stop until manually dismissed by user!
     */
    fun startContinuousAlarm(context: Context, locationTitle: String = "Designated Area") {
        if (isPlaying) return

        try {
            _alarmLocationTitle.value = locationTitle
            _isAlarmActive.value = true
            isPlaying = true

            // 1. Maximize Alarm Audio Stream Volume to 100%
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxAlarmVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                try {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
                } catch (e: Exception) {
                    Log.w("BuzzAlertHelper", "Could not set stream volume: ${e.message}")
                }
            }

            // 2. Start Continuous Repeating Vibration Loop
            startContinuousVibration(context)

            // 3. Start Continuous Alarm Audio Loop (MediaPlayer with USAGE_ALARM)
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            activeMediaPlayer?.release()
            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, alarmUri)
                isLooping = true // DOES NOT STOP AUTOMATICALLY!
                prepare()
                start()
            }

        } catch (e: Exception) {
            Log.e("BuzzAlertHelper", "Error starting continuous alarm: ${e.message}", e)
        }
    }

    /**
     * Manually stops the buzzing sound and cancels vibration.
     */
    fun stopAlarm(context: Context) {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Error stopping MediaPlayer: ${e.message}")
        }

        stopVibration(context)
        isPlaying = false
        _isAlarmActive.value = false
        _alarmLocationTitle.value = ""
    }

    private fun startContinuousVibration(context: Context) {
        // Pattern: [delay, vibrate, pause, vibrate, pause, vibrate...]
        val pattern = longArrayOf(0, 600, 300, 600, 300, 1000, 400)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                // repeat at index 0 = infinite loop until vibrator.cancel()
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Vibration loop error: ${e.message}")
        }
    }

    private fun stopVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Error cancelling vibration: ${e.message}")
        }
    }
}
