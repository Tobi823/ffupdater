package de.marmaro.krt.ffupdater.activity.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.R.color.holo_blue_dark
import android.R.color.holo_blue_light
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.*
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.activity.add.AddAppActivity
import de.marmaro.krt.ffupdater.DisplayableException
import de.marmaro.krt.ffupdater.activity.download.DownloadActivity
import de.marmaro.krt.ffupdater.FFUpdater.Companion.LOG_TAG
import de.marmaro.krt.ffupdater.Migrator
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.activity.settings.SettingsActivity
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.background.BackgroundWork
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.dialog.*
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.network.exceptions.ApiRateLimitExceededException
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour.*
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.notification.NotificationBuilder
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.settings.NoUnmeteredNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Keep
class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: MainRecyclerView
    private var firstStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        requestForNotificationPermissionIfNecessary()
        askForIgnoringBatteryOptimizationIfNecessary()

        val swipeContainer = findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        swipeContainer.setOnRefreshListener(userRefreshAppList)
        swipeContainer.setColorSchemeResources(holo_blue_light, holo_blue_dark)

        findViewById<MaterialToolbar>(R.id.materialToolbar).setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.main_view_toolbar__add_app -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        InstalledAppsCache.updateCache(applicationContext)
                        startActivity(AddAppActivity.createIntent(applicationContext))
                    }
                    true
                }
                R.id.main_view_toolbar__settings -> {
                    //start settings activity where we use select firefox product and release type;
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.main_view_toolbar__about -> {
                    val lastBackgroundUpdateCheckTime = DataStoreHelper.lastBackgroundCheck2
                    val lastBackgroundUpdateCheckText = if (lastBackgroundUpdateCheckTime != 0L) {
                        DateUtils.getRelativeDateTimeString(
                            this,
                            lastBackgroundUpdateCheckTime,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.WEEK_IN_MILLIS,
                            0
                        )
                    } else "/"
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.action_about_title)
                        .setMessage(getString(R.string.infobox, lastBackgroundUpdateCheckText))
                        .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                        .create()
                        .show()
                    true
                }
                else -> false
            }
        }

        initRecyclerView()
    }

    private var userRefreshAppList = OnRefreshListener {
        lifecycleScope.launch(Dispatchers.Main) {
            InstalledAppsCache.updateCache(applicationContext)
            showInstalledApps(FORCE_NETWORK)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main) { onResumeSuspended() }
    }

    @MainThread
    private suspend fun onResumeSuspended() {
        if (firstStart) InstalledAppsCache.updateCache(applicationContext)
        showInstalledApps(USE_CACHE)
        if (firstStart) startOrRestartBackgroundWork()
        firstStart = false
    }

    private fun askForIgnoringBatteryOptimizationIfNecessary() {
        if (DeviceSdkTester.supportsAndroid6M23() &&
            !BackgroundWork.isBackgroundUpdateCheckReliableExecuted()
        ) {
            NotificationBuilder.showBackgroundUpdateCheckUnreliableExecutionNotification(this)
        }
    }

    private fun initRecyclerView() {
        recyclerView = MainRecyclerView(this@MainActivity)
        val view = findViewById<RecyclerView>(R.id.main_activity__apps)
        view.adapter = recyclerView
        view.layoutManager = LinearLayoutManager(this@MainActivity)
    }

    private suspend fun showInstalledApps(cacheBehaviour: CacheBehaviour) {
        val correctFingerprintApps = InstalledAppsCache.getInstalledAppsWithCorrectFingerprint(applicationContext)
        val wrongFingerprintApps = InstalledAppsCache.getInstalledAppsWithDifferentFingerprint(applicationContext)
        recyclerView.notifyInstalledApps(
            correctFingerprintApps,
            if (ForegroundSettings.isHideAppsSignedByDifferentCertificate) listOf() else wrongFingerprintApps
        )
        fetchLatestUpdates(correctFingerprintApps, cacheBehaviour)
    }

    private suspend fun fetchLatestUpdates(apps: List<App>, cacheBehaviour: CacheBehaviour) {
        if (isNetworkMeterStatusOk()) {
            showErrorUnmeteredNetwork(apps)
            return
        }

        showLoadAnimationDuringExecution {
            apps.forEach {
                updateMetadataOf(it, cacheBehaviour)
            }
        }
    }

    private fun showErrorUnmeteredNetwork(apps: List<App>) {
        val e = NoUnmeteredNetworkException("Unmetered network is necessary but not available.")
        apps.forEach {
            recyclerView.notifyErrorForApp(it, R.string.main_activity__no_unmetered_network, e)
        }
        showBriefMessage(R.string.main_activity__no_unmetered_network)
    }

    private fun startOrRestartBackgroundWork() {
        if (Migrator.isBackgroundWorkRestartNecessary()) {
            BackgroundWork.forceRestart(this@MainActivity)
            Migrator.backgroundWorkHasBeenRestarted()
        } else {
            BackgroundWork.start(this@MainActivity)
        }
    }

    private suspend fun updateMetadataOf(app: App, cacheBehaviour: CacheBehaviour): InstalledAppStatus? {
        try {
            recyclerView.notifyAppChange(app, null)
            val updateStatus = app.findImpl().findInstalledAppStatus(applicationContext, cacheBehaviour)
            recyclerView.notifyAppChange(app, updateStatus)
            recyclerView.notifyClearedErrorForApp(app)
            return updateStatus
        } catch (e: Exception) {
            val textId = when (e) {
                is ApiRateLimitExceededException -> R.string.main_activity__github_api_limit_exceeded
                is NetworkException -> R.string.main_activity__temporary_network_issue
                is DisplayableException -> R.string.main_activity__an_error_occurred
                else -> R.string.main_activity__unexpected_error
            }
            recyclerView.notifyErrorForApp(app, textId, e)
            showBriefMessage(getString(textId))
            return null
        }
    }

    @MainThread
    fun installOrDownloadApp(app: App) {
        if (isNetworkMeterStatusOk()) {
            showBriefMessage(R.string.main_activity__no_unmetered_network)
            return
        }
        if (hasAppInstallPermission()) {
            RequestInstallationPermissionDialog().show(supportFragmentManager)
            return
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            // this may updates the app
            RunningDownloadsDialog(app).show(supportFragmentManager)
            return
        }
        Log.d(LOG_TAG, "MainActivity: Start DownloadActivity to install or update ${app.name}.")
        val intent = DownloadActivity.createIntent(this@MainActivity, app)
        startActivity(intent)
    }

    private fun hasAppInstallPermission() = DeviceSdkTester.supportsAndroid8Oreo26() && !packageManager.canRequestPackageInstalls()

    private fun isNetworkMeterStatusOk() = !ForegroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)

    @UiThread
    private fun showBriefMessage(message: Int) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    @UiThread
    private fun showBriefMessage(message: String) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    @UiThread
    private suspend fun showLoadAnimationDuringExecution(block: suspend () -> Unit) {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = true
        try {
            block()
        } finally {
            findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = false
        }
    }

    private fun requestForNotificationPermissionIfNecessary() {
        if (!DeviceSdkTester.supportsAndroid13T33()) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
            return
        }

        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            .launch(POST_NOTIFICATIONS)
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}

