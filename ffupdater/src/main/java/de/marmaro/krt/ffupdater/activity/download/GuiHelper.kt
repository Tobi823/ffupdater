package de.marmaro.krt.ffupdater.activity.download

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkNotSuitableException
import de.marmaro.krt.ffupdater.network.file.DownloadStatus
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.settings.InstallerSettings
import kotlinx.coroutines.channels.Channel

class GuiHelper(val activity: DownloadActivity) {

    fun show(vararg viewIds: Int) {
        viewIds.forEach { activity.findViewById<View>(it).visibility = View.VISIBLE }
    }

    fun hide(vararg viewIds: Int) {
        viewIds.forEach { activity.findViewById<View>(it).visibility = View.GONE }
    }

    fun setText(textId: Int, text: String) {
        activity.findViewById<TextView>(textId).text = text
    }

    @MainThread
    fun displayAppInstallationFailure(errorMessage: String, exception: Exception, appImpl: AppBase) {
        show(R.id.install_activity__exception)

        setText(
            R.id.install_activity__exception__text,
            activity.getString(R.string.application_installation_was_not_successful)
        )
        if (InstallerSettings.getInstallerMethod() == Installer.SESSION_INSTALLER) {
            show(R.id.install_activity__different_installer_info)
        }

        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        show(R.id.install_activity__exception__description)
        activity.findViewById<TextView>(R.id.install_activity__exception__description).text = errorMessage
        activity.findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = activity.getString(R.string.crash_report__explain_text__download_activity_install_file)
            val intent = CrashReportActivity.createIntent(activity, throwableAndLogs, description)
            activity.startActivity(intent)
        }

        val cacheFolder = appImpl.getApkCacheFolder(activity).absolutePath
        setText(R.id.install_activity__cache_folder_path, cacheFolder)
        if (!ForegroundSettings.isDeleteUpdateIfInstallFailed) {
            show(R.id.install_activity__delete_cache)
            show(R.id.install_activity__open_cache_folder)
        }
    }

    @MainThread
    fun displayDownloadFailure(status: InstalledAppStatus, exception: DisplayableException) {
        val description = if (exception is NetworkException) {
            activity.getString(R.string.install_activity__download_file_failed__crash_text)
        } else {
            exception.getNullSafeMessage()
        }
        show(R.id.install_activity__download_file_failed)
        setText(R.id.install_activity__download_file_failed__url, status.latestVersion.downloadUrl)
        setText(R.id.install_activity__download_file_failed__text, description)
        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        val text = activity.findViewById<TextView>(R.id.install_activity__download_file_failed__show_exception)
        text.setOnClickListener {
            val intent = CrashReportActivity.createIntent(activity, throwableAndLogs, description)
            activity.startActivity(intent)
        }
    }

    suspend fun showDownloadProgress(progressChannel: Channel<DownloadStatus>) {
        for (progress in progressChannel) {
            if (progress.progressInPercent != null) {
                activity.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress =
                    progress.progressInPercent
            }

            val text = when {
                progress.progressInPercent != null -> "(${progress.progressInPercent}%)"
                else -> "(${progress.totalMB}MB)"
            }
            val statusText = activity.getString(R.string.download_activity__download_app_with_status)
            setText(R.id.downloadingFileText, "$statusText $text")
        }
    }

    suspend fun showDownloadProgress2(progress: DownloadStatus) {
        if (progress.progressInPercent != null) {
            activity.findViewById<ProgressBar>(R.id.downloadingFileProgressBar).progress = progress.progressInPercent
        }

        val text = when {
            progress.progressInPercent != null -> "(${progress.progressInPercent}%)"
            else -> "(${progress.totalMB}MB)"
        }
        val statusText = activity.getString(R.string.download_activity__download_app_with_status)
        setText(R.id.downloadingFileText, "$statusText $text")
    }

    @MainThread
    fun displayFetchFailure(exception: DisplayableException) {
        val message = when (exception) {
            is ApiRateLimitExceededException -> activity.getString(R.string.download_activity__github_rate_limit_exceeded)
            is NetworkNotSuitableException -> exception.getNullSafeMessage()
            else -> activity.getString(R.string.download_activity__temporary_network_issue)
        }

        show(R.id.install_activity__exception)
        setText(R.id.install_activity__exception__text, message)
        val throwableAndLogs = ThrowableAndLogs(exception, LogReader.readLogs())
        activity.findViewById<TextView>(R.id.install_activity__exception__show_button).setOnClickListener {
            val description = activity.getString(R.string.crash_report__explain_text__download_activity_fetching_url)
            val intent = CrashReportActivity.createIntent(activity, throwableAndLogs, description)
            activity.startActivity(intent)
        }
    }

    fun resetGui() {
        hide(
            R.id.downloadedFile,
            R.id.downloadingFile,
            R.id.externalStorageNotAccessible,
            R.id.fetchUrl,
            R.id.fetchedUrlSuccess,
            R.id.fingerprintInstalledGood,
            R.id.install_activity__delete_cache,
            R.id.install_activity__different_installer_info,
            R.id.install_activity__download_file_failed,
            R.id.install_activity__exception,
            R.id.install_activity__exception__description,
            R.id.install_activity__open_cache_folder,
            R.id.install_activity__retry_installation,
            R.id.installerSuccess,
            R.id.installingApplication,
            R.id.tooLowMemory,
            R.id.useCachedDownloadedApk,
        )
    }
}
