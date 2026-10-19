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
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object BuzzAlertHelper {

    private var activeMediaPlayer: MediaPlayer? = null
    @Volatile
    private var isPlaying = false

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive = _isAlarmActive.asStateFlow()

    private val _alarmLocationTitle = MutableStateFlow("")
    val alarmLocationTitle = _alarmLocationTitle.asStateFlow()

    /**
     * Starts continuous full volume buzzing sound and repeating vibration
     * that will NOT stop until manually dismissed by user!
     */
    @Synchronized
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
                isLooping = true
                prepare()
                start()
            }

        } catch (e: Exception) {
            Log.e("BuzzAlertHelper", "Error starting continuous alarm: ${e.message}", e)
        }
    }

    /**
     * Manually stops the buzzing sound, cancels all vibrations across all managers,
     * and dismisses notification.
     */
    @Synchronized
    fun stopAlarm(context: Context) {
        isPlaying = false
        _isAlarmActive.value = false
        _alarmLocationTitle.value = ""

        // 1. Stop and release MediaPlayer audio
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            activeMediaPlayer = null
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Error stopping MediaPlayer: ${e.message}")
            activeMediaPlayer = null
        }

        // 2. Terminate all vibrations completely across all Android versions
        stopVibration(context)

        // 3. Cancel any lingering notifications from notification shade
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Error dismissing notifications: ${e.message}")
        }
    }

    private fun startContinuousVibration(context: Context) {
        val pattern = longArrayOf(0, 500, 250, 500, 250, 800, 300)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
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
            // Android 12+ (API 31+) VibratorManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.cancel()
                vm?.defaultVibrator?.cancel()
            }

            // Standard Vibrator service across all Android versions
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Error cancelling vibration: ${e.message}")
        }
    }
}
