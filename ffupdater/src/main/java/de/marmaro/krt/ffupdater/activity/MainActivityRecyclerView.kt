package de.marmaro.krt.ffupdater.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import de.marmaro.krt.ffupdater.MainActivity
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.AppWarningDialog
import de.marmaro.krt.ffupdater.dialog.CardviewOptionsDialog
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivityRecyclerView(private val activity: MainActivity) :
    RecyclerView.Adapter<MainActivityRecyclerView.AppHolder>() {

    @Keep
    private data class ExceptionWrapper(val message: Int, val exception: Exception)

    private var elements = listOf<App>()

    private var errors = mutableMapOf<App, ExceptionWrapper>()

    private var appsWithWrongFingerprint = listOf<App>()

    private var appAndUpdateStatus = mutableMapOf<App, InstalledAppStatus>()


    @UiThread
    @SuppressLint("NotifyDataSetChanged")
    fun notifyInstalledApps(appsWithCorrectFingerprint: List<App>, appsWithWrongFingerprint: List<App>) {
        val allElements = appsWithCorrectFingerprint + appsWithWrongFingerprint
        if (elements != allElements || this.appsWithWrongFingerprint != appsWithWrongFingerprint) {
            elements = allElements
            this.appsWithWrongFingerprint = appsWithWrongFingerprint
            notifyDataSetChanged()
        }
    }

    @UiThread
    @Throws(IllegalArgumentException::class)
    fun notifyAppChange(app: App, updateStatus: InstalledAppStatus?) {
        if (updateStatus == null) {
            appAndUpdateStatus.remove(app)
        } else {
            appAndUpdateStatus[app] = updateStatus
        }
        val index = elements.indexOf(app)
        require(index != -1)
        notifyItemChanged(index)
    }

    @UiThread
    @Throws(IllegalArgumentException::class)
    fun notifyErrorForApp(app: App, message: Int, exception: Exception) {
        errors[app] = ExceptionWrapper(message, exception)

        val index = elements.indexOf(app)
        require(index != -1)
        notifyItemChanged(index)
    }

    @UiThread
    @Throws(IllegalArgumentException::class)
    fun notifyClearedErrorForApp(app: App) {
        if (errors.containsKey(app)) {
            errors.remove(app)
            val index = elements.indexOf(app)
            require(index != -1)
            notifyItemChanged(index)
        }
    }

    inner class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewWithTag("appCardTitle")
        val icon: ImageView = itemView.findViewWithTag("appIcon")
        val warningIcon: ImageButton = itemView.findViewWithTag("appWarningButton")
        val eolReason: TextView = itemView.findViewWithTag("eolReason")
        val infoButton: ImageButton = itemView.findViewWithTag("appInfoButton")
        val openProjectPageButton: ImageButton = itemView.findViewWithTag("appOpenProjectPage")
        val installedVersion: TextView = itemView.findViewWithTag("appInstalledVersion")
        val availableVersion: TextView = itemView.findViewWithTag("appAvailableVersion")
        val downloadButton: ImageButton = itemView.findViewWithTag("appDownloadButton")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHolder {
        val inflater = LayoutInflater.from(parent.context)
        val appView = inflater.inflate(R.layout.activity_main_cardview, parent, false)
        return AppHolder(appView)
    }

    override fun onBindViewHolder(view: AppHolder, position: Int) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            val app = elements[position]
            val appImpl = app.findImpl()
            val metadata = appAndUpdateStatus.getOrDefault(app, null)
            val error = errors[app]
            val fragmentManager = activity.supportFragmentManager
            val hideWarningButtons = ForegroundSettings.isHideWarningButtonForInstalledApps
            val appHasWarning = appImpl.installationWarning == null

            view.title.setText(appImpl.title)
            view.icon.setImageResource(appImpl.icon)

            showAppInfo(view, app, error, metadata)

            view.downloadButton.setOnClickListener {
                activity.lifecycleScope.launch(Dispatchers.Main) {
                    activity.installOrDownloadApp(app)
                }
            }

            view.infoButton.setOnClickListener { CardviewOptionsDialog.newInstance(app).show(fragmentManager) }

            when {
                hideWarningButtons -> view.warningIcon.visibility = View.GONE
                appHasWarning -> view.warningIcon.visibility = View.GONE
                else -> AppWarningDialog.newInstanceOnClick(view.warningIcon, app, fragmentManager)
            }

            view.openProjectPageButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appImpl.projectPage))
                activity.startActivity(browserIntent)
            }
        }
    }

    private suspend fun showAppInfo(
        view: AppHolder,
        app: App,
        error: ExceptionWrapper?,
        metadata: InstalledAppStatus?,
    ) {
        when {
            appsWithWrongFingerprint.contains(app) -> showAppInfoForDifferentSignature(view, app)
            error != null -> showAppInfoForError(view, app, error)
            else -> showAppInfo(view, app, metadata)
        }
    }

    private fun showAppInfoForDifferentSignature(view: AppHolder, app: App) {
        showViews(listOf(view.installedVersion))
        hideViews(listOf(view.availableVersion, view.downloadButton, view.eolReason))
        view.installedVersion.text = activity.getString(app.findImpl().differentSignatureMessage)
    }

    private fun hideViews(elements: List<View>) {
        elements.forEach { it.visibility = View.GONE }
    }

    private fun showViews(elements: List<View>) {
        elements.forEach { it.visibility = View.VISIBLE }
    }

    private suspend fun showAppInfoForError(view: AppHolder, app: App, error: ExceptionWrapper) {
        showViews(listOf(view.installedVersion, view.availableVersion, view.downloadButton))
        val findImpl = app.findImpl()
        view.installedVersion.text = findImpl.getDisplayInstalledVersion(activity)
        view.availableVersion.setText(error.message)
        view.availableVersion.setOnClickListener {
            val description = activity.getString(R.string.crash_report__explain_text__download_activity_update_check)
            val context = activity.applicationContext
            val throwableAndLogs = ThrowableAndLogs(error.exception, LogReader.readLogs())
            val intent = CrashReportActivity.createIntent(context, throwableAndLogs, description)
            activity.startActivity(intent)
        }
        view.downloadButton.setImageResource(R.drawable.ic_file_download_grey)
        showOrHideEolReason(findImpl, view)
    }

    private suspend fun showAppInfo(
        view: AppHolder,
        app: App,
        metadata: InstalledAppStatus?,
    ) {
        showViews(listOf(view.installedVersion, view.availableVersion, view.downloadButton))
        val findImpl = app.findImpl()
        view.installedVersion.text = findImpl.getDisplayInstalledVersion(activity)
        view.availableVersion.text = getDisplayAvailableVersionWithAge(metadata)
        view.downloadButton.setImageResource(
            if (metadata?.isUpdateAvailable == true) {
                R.drawable.ic_file_download_orange
            } else {
                R.drawable.ic_file_download_grey
            }
        )
        showOrHideEolReason(findImpl, view)
    }

    private fun showOrHideEolReason(findImpl: AppBase, view: AppHolder) {
        if (findImpl.isEol()) {
            showViews(listOf(view.eolReason))
            view.eolReason.setText(findImpl.eolReason!!)
        } else {
            hideViews(listOf(view.eolReason))
        }
    }

    private fun getDisplayAvailableVersionWithAge(metadata: InstalledAppStatus?): String {
        val version = metadata?.displayVersion ?: "..."
        val dateString = metadata?.latestVersion?.publishDate ?: return version
        val date = try {
            ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        } catch (e: DateTimeException) {
            return version
        }
        val unixMillis = DateUtils.SECOND_IN_MILLIS * date.toEpochSecond()
        val min = Duration.ofMinutes(1).toMillis()
        val max = Duration.ofDays(100).toMillis()
        val relative = DateUtils.getRelativeDateTimeString(activity, unixMillis, min, max, 0)
        return "$version ($relative)"
    }

    override fun getItemCount(): Int {
        return elements.size
    }

}