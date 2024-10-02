package de.marmaro.krt.ffupdater.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import androidx.annotation.Keep
import androidx.core.content.getSystemService
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import java.time.Duration

@Keep
object PowerSaveModeReceiver : BroadcastReceiver() {
    private const val ATTRIBUTE_NAME = "power_manager__save_mode_enabled_timestamp"
    private val thresholdBetweenShortAndLongTime = Duration.ofHours(24)

    private lateinit var preferences: SharedPreferences
    private lateinit var powerManager: PowerManager

    enum class PowerSaveModeDuration {
        POWER_SAVE_MODE_NOT_ENABLED, ENABLED_RECENTLY, ENABLED_FOR_LONG_TIME
    }

    /**
     * For PowerManager.ACTION_POWER_SAVE_MODE_CHANGED it is required to register the receiver.
     */
    fun register(context: Context, sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
        powerManager = context.applicationContext.getSystemService<PowerManager>()!!

        if (powerManager.isPowerSaveMode && getTimeDurationOfEnabledPowerSaveMode().isZero) {
            storeThatPowerSaveModeIsEnabled()
        }

        val filter = IntentFilter()
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        context.applicationContext.registerReceiver(PowerSaveModeReceiver, filter)
    }

    fun getPowerSaveModeDuration(): PowerSaveModeDuration {
        val enableTimestamp = preferences.getLong(ATTRIBUTE_NAME, 0)
        if (!powerManager.isPowerSaveMode || enableTimestamp == 0L) {
            return PowerSaveModeDuration.POWER_SAVE_MODE_NOT_ENABLED
        }
        val duration = Duration.ofNanos(System.nanoTime() - enableTimestamp)
        if (duration <= thresholdBetweenShortAndLongTime) {
            return PowerSaveModeDuration.ENABLED_RECENTLY
        } else {
            return PowerSaveModeDuration.ENABLED_FOR_LONG_TIME
        }
    }

    @Deprecated("remove")
    fun isPowerSaveModeEnabledForShortTime(): Boolean {
        if (!powerManager.isPowerSaveMode) {
            return false
        }
        val timestamp = getTimeDurationOfEnabledPowerSaveMode()
        if (timestamp.isZero) {
            return false
        }
        return timestamp <= thresholdBetweenShortAndLongTime
    }

    @Deprecated("remove")
    fun isPowerSaveModeEnabledForLongerTime(): Boolean {
        if (!powerManager.isPowerSaveMode) {
            return false
        }
        val timestamp = getTimeDurationOfEnabledPowerSaveMode()
        if (timestamp.isZero) {
            return false
        }
        return timestamp > thresholdBetweenShortAndLongTime
    }

    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action != PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
            return
        }
        if (powerManager.isPowerSaveMode) {
            storeThatPowerSaveModeIsEnabled()
        } else {
            storeThatPowerSaveModeIsEnabled()
        }
    }

    private fun storeThatPowerSaveModeIsEnabled() {
        Log.i(LOG_TAG, "PowerSaveModeReceiver: Store that power save mode was enabled")
        preferences
                .edit()
                .putLong(ATTRIBUTE_NAME, System.nanoTime())
                .apply()
    }

    private fun storeThatPowerSaveModeIsDisabled() {
        Log.i(LOG_TAG, "PowerSaveModeReceiver: Store that power save mode was disabled")
        preferences
                .edit()
                .putLong(ATTRIBUTE_NAME, 0)
                .apply()
    }

    @Deprecated("remove")
    private fun getTimeDurationOfEnabledPowerSaveMode(): Duration {
        val timestamp = preferences.getLong(ATTRIBUTE_NAME, 0)
        if (timestamp == 0L) {
            return Duration.ZERO
        }
        return Duration.ofNanos(System.nanoTime() - timestamp)
    }
}