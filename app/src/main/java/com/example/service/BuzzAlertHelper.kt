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

    private val _ringingReminderId = MutableStateFlow(-1L)
    val ringingReminderId = _ringingReminderId.asStateFlow()

    private var activeVibratorManager: VibratorManager? = null
    private var activeVibrator: Vibrator? = null

    /**
     * Starts continuous full volume buzzing sound and repeating vibration
     * that will NOT stop until manually dismissed by user!
     */
    @Synchronized
    fun startContinuousAlarm(context: Context, locationTitle: String = "Designated Area", reminderId: Long = -1L) {
        if (isPlaying) return

        try {
            _ringingReminderId.value = reminderId
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
        val finishedReminderId = _ringingReminderId.value
        _ringingReminderId.value = -1L

        // 1. Stop and release MediaPlayer audio
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
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
            val notificationManager = NotificationManagerCompat.from(context)
            if (finishedReminderId != -1L) {
                notificationManager.cancel(finishedReminderId.toInt())
            }
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Error dismissing notifications: ${e.message}")
        }
    }

    private fun startContinuousVibration(context: Context) {
        val pattern = longArrayOf(0, 500, 250, 500, 250, 800, 300)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                activeVibratorManager = vm
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            }

            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            activeVibrator = vibrator
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
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
        // Cancel via stored active references
        try {
            activeVibratorManager?.cancel()
            activeVibratorManager?.defaultVibrator?.cancel()
            activeVibratorManager = null
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Active VibratorManager cancel error: ${e.message}")
            activeVibratorManager = null
        }

        try {
            activeVibrator?.cancel()
            activeVibrator = null
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "Active Vibrator cancel error: ${e.message}")
            activeVibrator = null
        }

        // Cancel via system services fresh lookup
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.cancel()
                vm?.defaultVibrator?.cancel()
            }
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "System VibratorManager cancel error: ${e.message}")
        }

        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w("BuzzAlertHelper", "System Vibrator cancel error: ${e.message}")
        }
    }
}
