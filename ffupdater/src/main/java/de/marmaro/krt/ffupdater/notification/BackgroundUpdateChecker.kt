package de.marmaro.krt.ffupdater.notification

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.marmaro.krt.ffupdater.app.AppList
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import java.util.*
import java.util.concurrent.ExecutionException
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
        } catch (exception: Exception) {
            showErrorNotification(exception)
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
            } catch (e: ExecutionException) {
                throw BackgroundUpdateCheckFailedException("fail to check $it", e)
            } catch (e: InterruptedException) {
                throw BackgroundUpdateCheckInterruptedException("fail to check $it", e)
            } catch (e: TimeoutException) {
                throw BackgroundUpdateCheckTimeoutException("fail to check $it", e)
            }
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        UpdateNotificationManager(context, notificationManager).showNotifications(appsWithUpdates)
    }

    private fun showErrorNotification(exception: Exception) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ErrorNotificationManager(context, notificationManager).showNotification(exception)
    }

    // TODO private val TIMEOUT = Duration.ofSeconds(30) muss noch implementiert werden

    open class BackgroundUpdateCheckException(message: String, throwable: Throwable) :
            Exception(message, throwable)
    class BackgroundUpdateCheckFailedException(message: String, throwable: Throwable) :
            BackgroundUpdateCheckException(message, throwable)
    class BackgroundUpdateCheckInterruptedException(message: String, throwable: Throwable) :
            BackgroundUpdateCheckException(message, throwable)
    class BackgroundUpdateCheckTimeoutException(message: String, throwable: Throwable) :
            BackgroundUpdateCheckException(message, throwable)
}