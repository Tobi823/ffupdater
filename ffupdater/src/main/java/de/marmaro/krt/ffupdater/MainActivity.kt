package de.marmaro.krt.ffupdater

import android.Manifest.permission.POST_NOTIFICATIONS
import android.R.color.holo_blue_dark
import android.R.color.holo_blue_light
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.crash.CrashReportActivity
import de.marmaro.krt.ffupdater.crash.LogReader
import de.marmaro.krt.ffupdater.crash.ThrowableAndLogs
import de.marmaro.krt.ffupdater.device.BatteryOptimizationsHelper
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.dialog.*
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.*
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.*

@Keep
class MainActivity : AppCompatActivity() {
    private lateinit var recycleViewAdapter: InstalledAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (CrashListener.openCrashReporterForUncaughtExceptions(applicationContext)) {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        requestForNotificationPermissionIfNecessary()
        BatteryOptimizationsHelper.disableBatteryOptimizationOnProblematicPhones(this)

        findViewById<View>(R.id.installAppButton).setOnClickListener {
            lifecycleScope.launch(Dispatchers.Default) {
                InstalledAppsCache.updateCache(applicationContext)
                val intent = AddAppActivity.createIntent(applicationContext)
                startActivity(intent)
            }
        }
        val swipeContainer = findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        swipeContainer.setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.Main) {
                InstalledAppsCache.updateCache(applicationContext)
                showInstalledAppsInRecyclerView(useNetworkCache = false)
            }
        }
        swipeContainer.setColorSchemeResources(holo_blue_light, holo_blue_dark)
        lifecycleScope.launch(Dispatchers.Default) {
            InstalledAppsCache.updateCache(applicationContext)
        }
        initRecyclerView()
    }

    @MainThread
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main) {
            showInstalledAppsInRecyclerView(useNetworkCache = true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_about) {
            val backgroundCheckString = DataStoreHelper.getLastBackgroundCheckString(applicationContext)
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.action_about_title)
                .setMessage(getString(R.string.infobox, backgroundCheckString))
                .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
                .show()
        } else if (itemId == R.id.action_settings) {
            //start settings activity where we use select firefox product and release type;
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        recycleViewAdapter = InstalledAppsAdapter(this@MainActivity)
        val view = findViewById<RecyclerView>(R.id.main_activity__apps)
        view.adapter = recycleViewAdapter
        view.layoutManager = LinearLayoutManager(this@MainActivity)
    }

    private suspend fun showInstalledAppsInRecyclerView(useNetworkCache: Boolean) {
        val appsWithCorrectFingerprint = InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(applicationContext)
        val appsWithWrongFingerprint = if (ForegroundSettings.isHideAppsSignedByDifferentCertificate) {
            listOf()
        } else {
            InstalledAppsCache.getInstalledAppsWithDifferentFingerprint(applicationContext)
        }
        recycleViewAdapter.notifyInstalledApps(
            appsWithCorrectFingerprint,
            appsWithWrongFingerprint
        )
        fetchLatestUpdates(appsWithCorrectFingerprint, useNetworkCache)
    }

    private suspend fun fetchLatestUpdates(apps: List<App>, useNetworkCache: Boolean) {
        if (!ForegroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            setLoadAnimationState(false)
            apps.forEach {
                recycleViewAdapter.notifyErrorForApp(it, R.string.main_activity__no_unmetered_network, null)
            }
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }

        setLoadAnimationState(true)
        val cacheBehaviour = if (useNetworkCache) USE_CACHE else FORCE_NETWORK
        apps.forEach {
            updateMetadataOf(it, cacheBehaviour)
        }
        setLoadAnimationState(false)
    }

    private suspend fun updateMetadataOf(app: App, cacheBehaviour: CacheBehaviour): InstalledAppStatus? {
        try {
            recycleViewAdapter.notifyAppChange(app, null)
            val updateStatus = app.findImpl().findInstalledAppStatus(applicationContext, cacheBehaviour)
            recycleViewAdapter.notifyAppChange(app, updateStatus)
            recycleViewAdapter.notifyClearedErrorForApp(app)
            return updateStatus
        } catch (e: ApiRateLimitExceededException) {
            recycleViewAdapter.notifyErrorForApp(app, R.string.main_activity__github_api_limit_exceeded, e)
            showToast(getString(R.string.main_activity__github_api_limit_exceeded))
        } catch (e: NetworkException) {
            recycleViewAdapter.notifyErrorForApp(app, R.string.main_activity__temporary_network_issue, e)
            showToast(getString(R.string.main_activity__temporary_network_issue))
        } catch (e: DisplayableException) {
            recycleViewAdapter.notifyErrorForApp(app, R.string.main_activity__an_error_occurred, e)
            showToast(getString(R.string.main_activity__an_error_occurred))
        }
        return null
    }

    @MainThread
    private suspend fun installOrDownloadApp(app: App) {
        if (!ForegroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }
        if (DeviceSdkTester.supportsAndroidOreo() && !packageManager.canRequestPackageInstalls()) {
            RequestInstallationPermissionDialog().show(supportFragmentManager)
            return
        }
        val metadata = updateMetadataOf(app, USE_CACHE) ?: return
        if (!metadata.isUpdateAvailable) {
            // this may displays RunningDownloadsDialog and updates the app
            InstallSameVersionDialog.newInstance(app).show(supportFragmentManager)
            return
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            // this may updates the app
            RunningDownloadsDialog.newInstance(app, false).show(supportFragmentManager)
            return
        }
        Log.d(LOG_TAG, "installOrDownloadApp(): Start DownloadActivity to install or update ${app.name}.")
        val intent = DownloadActivity.createIntent(this@MainActivity, app)
        startActivity(intent)
    }

    @UiThread
    private fun showToast(message: Int) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    @UiThread
    private fun showToast(message: String) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    @UiThread
    private fun setLoadAnimationState(visible: Boolean) {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = visible
    }

    private fun requestForNotificationPermissionIfNecessary() {
        if (!DeviceSdkTester.supportsAndroid13() ||
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED
        ) {
            return
        }

        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            .launch(POST_NOTIFICATIONS)
    }

    class InstalledAppsAdapter(private val activity: MainActivity) :
        RecyclerView.Adapter<InstalledAppsAdapter.AppHolder>() {

        @Keep
        private data class ExceptionWrapper(val message: Int, val exception: Exception?)

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
        fun notifyErrorForApp(app: App, message: Int, exception: Exception?) {
            errors[app] = ExceptionWrapper(message, exception)

            val index = elements.indexOf(app)
            require(index != -1)
            notifyItemChanged(index)
        }

        @UiThread
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
            val app = elements[position]
            val appImpl = app.findImpl()
            val metadata = appAndUpdateStatus.getOrDefault(app, null)
            val error = errors[app]
            val fragmentManager = activity.supportFragmentManager

            view.title.setText(appImpl.title)
            view.icon.setImageResource(appImpl.icon)

            when {
                appsWithWrongFingerprint.contains(app) -> onBindViewHolderWhenWrongFingerprint(view)
                error != null -> onBindViewHolderWhenError(view, app, error)
                else -> onBindViewHolderWhenNormal(view, app, metadata)
            }

            view.downloadButton.setOnClickListener {
                activity.lifecycleScope.launch(Dispatchers.Main) {
                    activity.installOrDownloadApp(app)
                }
            }

            val hideWarningButtons = ForegroundSettings.isHideWarningButtonForInstalledApps
            when {
                hideWarningButtons -> view.warningIcon.visibility = View.GONE
                appImpl.installationWarning == null -> view.warningIcon.visibility = View.GONE
                else -> AppWarningDialog.newInstanceOnClick(view.warningIcon, app, fragmentManager)
            }

            AppInfoDialog.newInstanceOnClick(view.infoButton, app, fragmentManager)

            view.openProjectPageButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appImpl.projectPage))
                activity.startActivity(browserIntent)
            }
        }

        private fun onBindViewHolderWhenWrongFingerprint(view: AppHolder) {
            view.installedVersion.text =
                activity.getString(R.string.main_activity__app_was_signed_by_different_certificate)
            view.availableVersion.text = ""
            view.downloadButton.visibility = View.GONE
            view.availableVersion.visibility = View.GONE
            view.eolReason.visibility = View.GONE
        }

        private fun onBindViewHolderWhenError(view: AppHolder, app: App, error: ExceptionWrapper) {
            view.installedVersion.visibility = View.VISIBLE
            view.installedVersion.text = app.findImpl().getDisplayInstalledVersion(activity)
            view.availableVersion.visibility = View.VISIBLE
            view.availableVersion.setText(error.message)
            if (error.exception != null) {
                val throwableAndLogs = ThrowableAndLogs(error.exception, LogReader.readLogs())
                view.availableVersion.setOnClickListener {
                    val description =
                        activity.getString(R.string.crash_report__explain_text__download_activity_update_check)
                    val intent =
                        CrashReportActivity.createIntent(activity.applicationContext, throwableAndLogs, description)
                    activity.startActivity(intent)
                }
            }
            view.downloadButton.setImageResource(R.drawable.ic_file_download_grey)
            view.eolReason.visibility = if (app.findImpl().isEol()) View.VISIBLE else View.GONE
            app.findImpl().eolReason?.let { view.eolReason.setText(it) }
        }

        private fun onBindViewHolderWhenNormal(view: AppHolder, app: App, metadata: InstalledAppStatus?) {
            view.installedVersion.visibility = View.VISIBLE
            view.installedVersion.text = app.findImpl().getDisplayInstalledVersion(activity)
            view.availableVersion.visibility = View.VISIBLE
            view.availableVersion.text = getDisplayAvailableVersionWithAge(metadata)
            view.downloadButton.setImageResource(
                if (metadata?.isUpdateAvailable == true) {
                    R.drawable.ic_file_download_orange
                } else {
                    R.drawable.ic_file_download_grey
                }
            )
            view.eolReason.visibility = if (app.findImpl().isEol()) View.VISIBLE else View.GONE
            app.findImpl().eolReason?.let { view.eolReason.setText(it) }
        }


        private fun getDisplayAvailableVersionWithAge(metadata: InstalledAppStatus?): String {
            val version = metadata?.displayVersion ?: "..."
            val dateString = metadata?.latestVersion?.publishDate ?: return version
            val date = try {
                ZonedDateTime.parse(dateString, ISO_ZONED_DATE_TIME)
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

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}

