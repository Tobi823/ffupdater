package de.marmaro.krt.ffupdater.activity.main

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
import de.marmaro.krt.ffupdater.dialog.CardviewOptionsDialog
import de.marmaro.krt.ffupdater.dialog.CardviewOptionsDialog.Companion.APP_WAS_HIDDEN
import de.marmaro.krt.ffupdater.dialog.CardviewOptionsDialog.Companion.AUTO_UPDATE_CHANGED
import de.marmaro.krt.ffupdater.settings.BackgroundSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections

class MainRecyclerView(private val activity: MainActivity) : RecyclerView.Adapter<MainRecyclerView.AppHolder>() {

    @Keep
    private data class ExceptionWrapper(val message: Int, val exception: Exception)

    private var elements = mutableListOf<App>()
    private var errors = mutableMapOf<App, ExceptionWrapper>()
    private var appsWithWrongFingerprint = setOf<App>()
    private var appsWithCorrectFingerprint = setOf<App>()
    private var appAndUpdateStatus = mutableMapOf<App, InstalledAppStatus>()

    @UiThread
    @SuppressLint("NotifyDataSetChanged")
    suspend fun notifyInstalledApps(appsWithCorrectFingerprint: List<App>, appsWithWrongFingerprint: List<App>) {
        val sortedAppsWithCorrectFingerprint = sortApps(appsWithCorrectFingerprint)
        val allElements = sortedAppsWithCorrectFingerprint + appsWithWrongFingerprint
        if (elements != allElements) {
            elements = allElements.toMutableList()
            this.appsWithWrongFingerprint = appsWithWrongFingerprint.toSet()
            this.appsWithCorrectFingerprint = sortedAppsWithCorrectFingerprint.toSet()
            notifyDataSetChanged()
        }
    }

    @UiThread
    @Throws(IllegalArgumentException::class)
    suspend fun notifyAppChange(app: App, updateStatus: InstalledAppStatus?) {
        if (updateStatus == null) {
            appAndUpdateStatus.remove(app)
        } else {
            appAndUpdateStatus[app] = updateStatus
        }
        val index = elements.indexOf(app)
        require(index != -1)
        notifyItemChanged(index)
        sortAppsByUpdateAvailabilityAndName(app)
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

    private suspend fun sortAppsByUpdateAvailabilityAndName(app: App) {
        val index = elements.indexOf(app)
        require(index != -1)

        val newOrder = sortApps(appsWithCorrectFingerprint.toList())
        val newOrderItemIndex = elements.indexOf(newOrder[index])
        require(newOrderItemIndex != -1)
        if (newOrderItemIndex == index) {
            return // skip because nothing changes
        }

        require(newOrderItemIndex != -1)
        Collections.swap(elements, index, newOrderItemIndex)
        notifyItemChanged(index)
        notifyItemChanged(newOrderItemIndex)
    }

    suspend fun sortAppsByUpdateAvailabilityAndName() {
        appsWithCorrectFingerprint.forEach {
            sortAppsByUpdateAvailabilityAndName(it)
        }
    }

    private fun convertToValue(status: InstalledAppStatus?): Int {
        if (status == null) {
            return 2
        }
        if (status.isUpdateAvailable) {
            return 1
        }
        return 3
    }

    private suspend fun sortApps(apps: List<App>): MutableList<App> {
        val installStatus = apps.map {
            activity.lifecycleScope.async {
                it to (appAndUpdateStatus[it] ?: it.findImpl().tryGetOldCache(activity))
            }
        }.toList().awaitAll()

        return installStatus.stream() //
            .sorted(compareBy({ convertToValue(it.second) }, { activity.getString(it.first.findImpl().title) }))
            .map { it.first } //
            .toList()
    }

    inner class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewWithTag("appCard")
        val title: TextView = itemView.findViewWithTag("appCardTitle")
        val icon: ImageView = itemView.findViewWithTag("appIcon")
        val infoButton: ImageButton = itemView.findViewWithTag("appInfoButton")
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
        val app = elements[position]
        val appImpl = app.findImpl()

        configureTitle(view, appImpl)
        configureIcon(view, appImpl)
        activity.lifecycleScope.launch(Dispatchers.Main) {
            configureInstalledVersion(view, appImpl)
            configureAvailableVersion(view, appImpl)
        }
        configureDownloadButton(view, appImpl)
        configureInfoButton(view, appImpl, activity.supportFragmentManager)
        setCardColor(view, appImpl)
    }

    private fun configureTitle(view: AppHolder, appImpl: AppBase) {
        view.title.setText(appImpl.title)
    }

    private fun configureIcon(view: AppHolder, appImpl: AppBase) {
        view.icon.setImageResource(appImpl.icon)
    }

    private suspend fun configureInstalledVersion(view: AppHolder, appImpl: AppBase) {
        view.installedVersion.text = appImpl.getDisplayInstalledVersion(activity)
    }

    private fun configureAvailableVersion(view: AppHolder, appImpl: AppBase) {
        val error = errors[appImpl.app]
        if (error != null) {
            view.availableVersion.setText(error.message)
            view.availableVersion.setOnClickListener { startCrashActivity(error) }
        } else {
            val metadata = appAndUpdateStatus.getOrDefault(appImpl.app, null)
            view.availableVersion.text = getDisplayAvailableVersionWithAge(metadata)
        }
        view.availableVersion.visibility = if (appImpl.app in appsWithWrongFingerprint) View.GONE else View.VISIBLE
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

    private fun startCrashActivity(error: ExceptionWrapper) {
        val description = activity.getString(R.string.crash_report__explain_text__download_activity_update_check)
        val context = activity.applicationContext
        val throwableAndLogs = ThrowableAndLogs(error.exception, LogReader.readLogs())
        val intent = CrashReportActivity.createIntent(context, throwableAndLogs, description)
        activity.startActivity(intent)
    }

    private fun configureDownloadButton(view: AppHolder, appImpl: AppBase) {
        val metadata = appAndUpdateStatus.getOrDefault(appImpl.app, null)
        view.downloadButton.visibility = if (metadata?.isUpdateAvailable == true) View.VISIBLE else View.GONE
        view.downloadButton.setOnClickListener { activity.installOrDownloadApp(appImpl.app) }
    }


    private fun configureInfoButton(view: AppHolder, appImpl: AppBase, fragmentManager: FragmentManager) {
        view.infoButton.setOnClickListener { showDialog(appImpl.app, fragmentManager) }
    }

    private fun showDialog(app: App, fragmentManager: FragmentManager) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            val dialog = CardviewOptionsDialog.create(app)
            dialog.show(fragmentManager)
            dialog.setFragmentResultListener(AUTO_UPDATE_CHANGED) { _, _ ->
                notifyItemChanged(elements.indexOf(app))
            }
            dialog.setFragmentResultListener(APP_WAS_HIDDEN) { _, _ ->
                notifyItemRemoved(elements.indexOf(app))
                elements.remove(app)
            }
        }
    }

    private fun setCardColor(
        view: AppHolder,
        appImpl: AppBase,
    ) {
        val app = appImpl.app
        val backgroundTintColor = when {
            appImpl.isEol() -> R.color.cardview_options__eol__background_tint_color
            app in appsWithWrongFingerprint -> R.color.cardview_options__different_signature__background_tint_color
            app in BackgroundSettings.excludedAppsFromUpdateCheck -> R.color.cardview_options__no_auto_updates__background_tint_color
            appImpl.wasInstalledByOtherApp(activity) -> R.color.cardview_options__not_installed_by_ffupdater__background_tint_color
            else -> R.color.main_activity__cardview_background_color
        }
        val color = ContextCompat.getColor(activity, backgroundTintColor)
        view.card.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun getItemCount(): Int {
        return elements.size
    }

}