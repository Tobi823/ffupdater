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
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus.INSTALLED
import de.marmaro.krt.ffupdater.app.entity.InstallationStatus.INSTALLED_WITH_DIFFERENT_FINGERPRINT
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.dialog.*
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.FileDownloader.CacheBehaviour.*
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.DateTimeException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var foregroundSettings: ForegroundSettingsHelper
    private lateinit var recycleViewAdapter: InstalledAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        StrictModeSetup.INSTANCE.enableStrictMode()
        foregroundSettings = ForegroundSettingsHelper(this)
        AppCompatDelegate.setDefaultNightMode(foregroundSettings.themePreference)
        Migrator().migrate(this)
        requestForNotificationPermissionIfNecessary()

        findViewById<View>(R.id.installAppButton).setOnClickListener {
            val intent = AddAppActivity.createIntent(this)
            startActivity(intent)
        }
        val swipeContainer = findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        swipeContainer.setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.Main) {
                updateMetadataOfApps(false)
            }
        }
        swipeContainer.setColorSchemeResources(holo_blue_light, holo_blue_dark)

        initRecyclerView()
    }

    @MainThread
    override fun onResume() {
        super.onResume()
        showInstalledAppsInRecyclerView()
        lifecycleScope.launch(Dispatchers.Main) {
            updateMetadataOfApps(true)
        }
    }

    override fun onStop() {
        super.onStop()
        BackgroundJob.initBackgroundUpdateCheck(this@MainActivity)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_about) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.action_about_title)
                .setMessage(getString(R.string.infobox, DataStoreHelper(this).lastBackgroundCheckString))
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

    private fun showInstalledAppsInRecyclerView() {
        runBlocking {
            val items = App.values()
                .groupBy { it.impl.isInstalled(applicationContext) }
            val installedCorrectFingerprint = items[INSTALLED] ?: listOf()
            val installedWrongFingerprint = if (foregroundSettings.isHideAppsSignedByDifferentCertificate) {
                listOf()
            } else {
                items[INSTALLED_WITH_DIFFERENT_FINGERPRINT] ?: listOf()
            }
            recycleViewAdapter.notifyInstalledApps(installedCorrectFingerprint, installedWrongFingerprint)
        }
    }

    @MainThread
    private suspend fun updateMetadataOfApps(useCache: Boolean = true) {
        val apps = App.values()
            .filter { DeviceAbiExtractor.INSTANCE.supportsOneOf(it.impl.supportedAbis) }
            .filter { it.impl.isInstalled(this@MainActivity) == INSTALLED }

        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            setLoadAnimationState(false)
            val errorText = getString(R.string.main_activity__no_unmetered_network)
            apps.forEach {
                recycleViewAdapter.notifyErrorForApp(it, errorText, null)
            }
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }

        setLoadAnimationState(true)
        val network = NetworkSettingsHelper(applicationContext)
        val cacheBehaviour = if (useCache) USE_CACHE_IF_NOT_TOO_OLD else FORCE_NETWORK
        val fileDownloader = FileDownloader(network, applicationContext, cacheBehaviour)
        apps.forEach {
            updateMetadataOf(it, fileDownloader)
        }
        setLoadAnimationState(false)
    }

    @MainThread
    private suspend fun updateMetadataOf(app: App, fileDownloader: FileDownloader) {
        try {
            recycleViewAdapter.notifyAppChange(app, null)
            val updateStatus = app.impl.findAppUpdateStatus(applicationContext, fileDownloader)
            requireNotNull(updateStatus) { "impossible because of USE_CACHE_IF_NOT_TOO_OLD / FORCE_NETWORK" }
            recycleViewAdapter.notifyAppChange(app, updateStatus)
        } catch (e: ApiRateLimitExceededException) {
            recycleViewAdapter.notifyErrorForApp(
                app, getString(R.string.main_activity__github_api_limit_exceeded), e
            )
            return
        } catch (e: NetworkException) {
            recycleViewAdapter.notifyErrorForApp(
                app, getString(R.string.main_activity__temporary_network_issue), e
            )
            return
        } catch (e: Exception) {
            recycleViewAdapter.notifyErrorForApp(app, getString(R.string.available_version_error), e)
            return
        }

        recycleViewAdapter.notifyClearedErrorForApp(app)
    }

    @MainThread
    private suspend fun installOrDownloadApp(app: App) {
        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }
        if (DeviceSdkTester.INSTANCE.supportsAndroidOreo() && !packageManager.canRequestPackageInstalls()) {
            RequestInstallationPermissionDialog().show(supportFragmentManager)
            return
        }
        val network = NetworkSettingsHelper(applicationContext)
        val fileDownloader = FileDownloader(network, applicationContext, USE_CACHE_IF_NOT_TOO_OLD)
        val metadata = app.impl.findAppUpdateStatus(this, fileDownloader)
        if (metadata?.isUpdateAvailable == false) {
            // this may displays RunningDownloadsDialog and updates the app
            InstallSameVersionDialog.newInstance(app).show(supportFragmentManager)
            return
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            // this may updates the app
            RunningDownloadsDialog.newInstance(app, false).show(supportFragmentManager)
            return
        }
        val intent = DownloadActivity.createIntent(this@MainActivity, app)
        startActivity(intent)
    }

    @UiThread
    private fun showToast(message: Int) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    @UiThread
    private fun setLoadAnimationState(visible: Boolean) {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = visible
    }

    private fun requestForNotificationPermissionIfNecessary() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            return
        }

        val request =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (!isGranted) {
                    Snackbar.make(
                        findViewById(R.id.coordinatorLayout),
                        R.string.main_activity__denied_notification_permission,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        request.launch(POST_NOTIFICATIONS)
    }

    class InstalledAppsAdapter(private val activity: MainActivity) :
        RecyclerView.Adapter<InstalledAppsAdapter.AppHolder>() {

        private data class ExceptionWrapper(val message: String, val exception: Exception?)

        private var elements = listOf<App>()

        private var errors = mutableMapOf<App, ExceptionWrapper>()

        private var appsWithWrongFingerprint = listOf<App>()

        private var appAndUpdateStatus = mutableMapOf<App, AppUpdateStatus>()


        @SuppressLint("NotifyDataSetChanged")
        fun notifyInstalledApps(appsWithCorrectFingerprint: List<App>, appsWithWrongFingerprint: List<App>) {
            val allElements = appsWithCorrectFingerprint + appsWithWrongFingerprint
            if (elements != allElements || this.appsWithWrongFingerprint != appsWithWrongFingerprint) {
                elements = allElements
                this.appsWithWrongFingerprint = appsWithWrongFingerprint
                notifyDataSetChanged()
            }
        }

        fun notifyAppChange(app: App, updateStatus: AppUpdateStatus?) {
            if (updateStatus == null) {
                appAndUpdateStatus.remove(app)
            } else {
                appAndUpdateStatus[app] = updateStatus
            }
            val index = elements.indexOf(app)
            require(index != -1)
            notifyItemChanged(index)
        }

        fun notifyErrorForApp(app: App, message: String, exception: Exception?) {
            errors[app] = ExceptionWrapper(message, exception)

            val index = elements.indexOf(app)
            require(index != -1)
            notifyItemChanged(index)
        }

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
            val metadata = appAndUpdateStatus.getOrDefault(app, null)
            val error = errors[app]
            val fragmentManager = activity.supportFragmentManager

            view.title.setText(app.impl.title)
            view.icon.setImageResource(app.impl.icon)

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

            val hideWarningButtons = activity.foregroundSettings.isHideWarningButtonForInstalledApps
            when {
                hideWarningButtons -> view.warningIcon.visibility = View.GONE
                app.impl.installationWarning == null -> view.warningIcon.visibility = View.GONE
                else -> AppWarningDialog.newInstanceOnClick(view.warningIcon, app, fragmentManager)
            }

            AppInfoDialog.newInstanceOnClick(view.infoButton, app, fragmentManager)

            view.openProjectPageButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.impl.projectPage))
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
            view.installedVersion.text = app.impl.getDisplayInstalledVersion(activity)
            view.availableVersion.text = error.message
            if (error.exception != null) {
                view.availableVersion.setOnClickListener {
                    val description =
                        activity.getString(R.string.crash_report__explain_text__download_activity_update_check)
                    val intent = CrashReportActivity.createIntent(activity, error.exception, description)
                    activity.startActivity(intent)
                }
            }
            view.downloadButton.setImageResource(R.drawable.ic_file_download_grey)
            view.eolReason.visibility = if (app.impl.isEol()) View.VISIBLE else View.GONE
            app.impl.eolReason?.let { view.eolReason.setText(it) }
        }

        private fun onBindViewHolderWhenNormal(view: AppHolder, app: App, metadata: AppUpdateStatus?) {
            view.installedVersion.text = app.impl.getDisplayInstalledVersion(activity)
            view.availableVersion.text = getDisplayAvailableVersionWithAge(metadata)
            view.downloadButton.setImageResource(
                if (metadata?.isUpdateAvailable == true) {
                    R.drawable.ic_file_download_orange
                } else {
                    R.drawable.ic_file_download_grey
                }
            )
            view.eolReason.visibility = if (app.impl.isEol()) View.VISIBLE else View.GONE
            app.impl.eolReason?.let { view.eolReason.setText(it) }
        }


        private fun getDisplayAvailableVersionWithAge(metadata: AppUpdateStatus?): String {
            val version = metadata?.displayVersion ?: "..."
            val dateString = metadata?.latestUpdate?.publishDate ?: return version
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
        const val LOG_TAG = "MainActivity"
        fun createIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}

