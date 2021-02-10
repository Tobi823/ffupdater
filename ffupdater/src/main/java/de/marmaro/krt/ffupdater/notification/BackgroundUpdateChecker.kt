package de.marmaro.krt.ffupdater.notification

import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import de.marmaro.krt.ffupdater.app.AppList
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeoutException

/**
 * This class will call the [WorkManager] to check regularly for app updates in the background.
 * When an app update is available, a notification will be displayed.
 */
class BackgroundUpdateChecker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            doBackgroundCheck()
            Result.success()
        } catch (e: Exception) {
            ErrorNotificationBuilder.showNotification(applicationContext, e)
            Result.failure()
        }
    }

    private suspend fun doBackgroundCheck() {
        val device = DeviceEnvironment().abis[0] //TODO
        val context = applicationContext
        val disabledApps = SettingsHelper(context).disabledApps
        val appsForChecking = AppList.values()
                .filter { !disabledApps.contains(it) }
                .filter { it.impl.isInstalled(context) }
        val appsWithUpdates = appsForChecking.filter {
            try {
                val result: UpdateCheckResult = it.impl.updateCheckAsync(context, device).await()
                result.isUpdateAvailable
            } catch (e: Exception) {
                throw BackgroundUpdateCheckFailedException("fail to check $it", e)
            }
        }
        UpdateNotificationBuilder.showNotifications(appsWithUpdates, context)
    }

    companion object {
        private const val WORK_MANAGER_KEY: String = "update_checker"

        fun startOrStopBackgroundUpdateCheck(context: Context) {
            if (SettingsHelper(context).automaticCheck) {
                startBackgroundUpdateCheck(context)
            } else {
                stopBackgroundUpdateCheck(context)
            }
        }

        private fun startBackgroundUpdateCheck(context: Context) {
            val repeatInterval = SettingsHelper(context).checkInterval
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            val saveRequest = PeriodicWorkRequest.Builder(
                    BackgroundUpdateChecker::class.java, repeatInterval.toMinutes(), MINUTES)
                    .setConstraints(constraints)
                    .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(WORK_MANAGER_KEY, REPLACE, saveRequest)
        }

        private fun stopBackgroundUpdateCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_MANAGER_KEY)
        }
    }

    // TODO private val TIMEOUT = Duration.ofSeconds(30) muss noch implementiert werden

    class BackgroundUpdateCheckFailedException(message: String, throwable: Throwable) :
            Exception(message, throwable)
}