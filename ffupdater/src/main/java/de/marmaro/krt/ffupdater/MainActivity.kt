package de.marmaro.krt.ffupdater

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.R.string.crash_report__explain_text__main_activity_update_check
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.background.BackgroundJob
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.BuildMetadata
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.dialog.*
import de.marmaro.krt.ffupdater.download.AppDownloadStatus.Companion.areDownloadsInBackgroundActive
import de.marmaro.krt.ffupdater.download.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private val sameAppVersionIsAlreadyInstalled: EnumMap<App, Boolean> = EnumMap(App::class.java)
    private val availableVersions: EnumMap<App, TextView> = EnumMap(App::class.java)
    private val downloadButtons: EnumMap<App, ImageButton> = EnumMap(App::class.java)
    private val errorsDuringUpdateCheck: EnumMap<App, Exception?> = EnumMap(App::class.java)
    private lateinit var foregroundSettings: ForegroundSettingsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashListener.openCrashReporterForUncaughtExceptions(this)

        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (BuildMetadata.isDebugBuild()) {
            StrictModeSetup.enableStrictMode()
        }
        foregroundSettings = ForegroundSettingsHelper(this)
        AppCompatDelegate.setDefaultNightMode(foregroundSettings.themePreference)
        Migrator(this).migrate()

        val deviceAbis = DeviceAbiExtractor.findSupportedAbis()
        findViewById<View>(R.id.installAppButton).setOnClickListener {
            val dialog = InstallNewAppDialog.newInstance(deviceAbis)
            dialog.show(supportFragmentManager)
        }
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.Main) {
                checkForUpdates()
            }
        }
    }

    @MainThread
    override fun onResume() {
        super.onResume()
        initUI()
        lifecycleScope.launch(Dispatchers.Main) {
            checkForUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        cleanUpObjects()
    }

    override fun onStop() {
        super.onStop()
        BackgroundJob.startOrStopBackgroundUpdateCheck(this@MainActivity)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_about) {
            val timestamp = DataStoreHelper(this).lastBackgroundCheck
                ?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(it) }
                ?: "/"
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.action_about_title)
                .setMessage(getString(R.string.infobox, timestamp))
                .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
                .show()
        } else if (itemId == R.id.action_settings) {
            //start settings activity where we use select firefox product and release type;
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    @UiThread
    private fun cleanUpObjects() {
        availableVersions.clear()
        downloadButtons.clear()
        errorsDuringUpdateCheck.clear()
    }

    @UiThread
    private fun initUI() {
        val mainLayout = findViewById<LinearLayout>(R.id.mainLinearLayout)
        mainLayout.removeAllViews()
        cleanUpObjects()
        val installedApps = App.values().filter { it.detail.isInstalled(this) }
        installedApps.forEach { app ->
            val newCardView = layoutInflater.inflate(R.layout.app_card_layout, mainLayout, false)

            val installedVersion: TextView = newCardView.findViewWithTag("appInstalledVersion")
            installedVersion.text = app.detail.getDisplayInstalledVersion(this)

            val availableVersion: TextView = newCardView.findViewWithTag("appAvailableVersion")
            availableVersions[app] = availableVersion
            availableVersion.setOnClickListener {
                val exception = errorsDuringUpdateCheck[app]
                if (exception != null) {
                    val description = getString(crash_report__explain_text__main_activity_update_check)
                    val intent = CrashReportActivity.createIntent(this, exception, description)
                    startActivity(intent)
                }
            }

            val downloadButton: ImageButton = newCardView.findViewWithTag("appDownloadButton")
            downloadButton.setOnClickListener {
                if (sameAppVersionIsAlreadyInstalled[app] == true) {
                    InstallSameVersionDialog.newInstance(app).show(supportFragmentManager)
                } else {
                    installApp(app, askForConfirmationIfOtherDownloadsAreRunning = true)
                }
            }
            downloadButtons[app] = downloadButton
            setDownloadButtonState(app, false)

            val infoButton: ImageButton = newCardView.findViewWithTag("appInfoButton")
            infoButton.setOnClickListener {
                AppInfoDialog.newInstance(app).show(supportFragmentManager)
            }

            val cardTitle: TextView = newCardView.findViewWithTag("appCardTitle")
            cardTitle.setText(app.detail.displayTitle)

            val icon = newCardView.findViewWithTag<ImageView>("appIcon")
            icon.setImageResource(app.detail.displayIcon)

            mainLayout.addView(newCardView)
        }
    }

    @MainThread
    private suspend fun checkForUpdates() {
        val apps = App.values()
            .filter { it.detail.isInstalled(this@MainActivity) }

        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            setAvailableVersion(apps, getString(R.string.main_activity__no_unmetered_network))
            setLoadAnimationState(false)
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }

        setLoadAnimationState(true)
        setAvailableVersion(apps, getString(R.string.available_version_loading))
        apps.forEach { checkForAppUpdate(it) }
        setLoadAnimationState(false)
    }

    @MainThread
    private suspend fun checkForAppUpdate(app: App) {
        try {
            val updateResult = app.detail.updateCheck(applicationContext)
            setAvailableVersion(app, updateResult.displayVersion)
            sameAppVersionIsAlreadyInstalled[app] = !updateResult.isUpdateAvailable
            if (updateResult.isUpdateAvailable) {
                setDownloadButtonState(app, true)
            } else {
                setDownloadButtonState(app, false)
            }
        } catch (e: GithubRateLimitExceededException) {
            showUpdateCheckError(app, R.string.main_activity__github_api_limit_exceeded, e)
        } catch (e: NetworkException) {
            showUpdateCheckError(app, R.string.main_activity__temporary_network_issue, e)
        } catch (e: Exception) {
            showUpdateCheckError(app, R.string.available_version_error, e)
        }
    }

    @MainThread
    private fun setAvailableVersion(app: App, message: String) {
        // it is possible that MainActivity is destroyed but the coroutine wants to update
        // the "available version" text field (which does not longer exists)
        availableVersions[app]?.text = message
        errorsDuringUpdateCheck[app] = null
    }

    @MainThread
    private fun setAvailableVersion(apps: List<App>, message: String) {
        apps.forEach { setAvailableVersion(it, message) }
    }

    @UiThread
    private fun showUpdateCheckError(app: App, message: Int, exception: Exception) {
        errorsDuringUpdateCheck[app] = exception
        availableVersions[app]?.setText(message)
        setDownloadButtonState(app, false)
    }

    @MainThread
    fun installApp(app: App, askForConfirmationIfOtherDownloadsAreRunning: Boolean = false) {
        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }
        if (DeviceSdkTester.supportsAndroidOreo() && !packageManager.canRequestPackageInstalls()) {
            RequestInstallationPermissionDialog().show(supportFragmentManager)
            return
        }
        if (askForConfirmationIfOtherDownloadsAreRunning && areDownloadsInBackgroundActive()) {
            RunningDownloadsDialog.newInstance(app).show(supportFragmentManager)
            return
        }

        val intent = Intent(this@MainActivity, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        startActivity(intent)
    }

    @UiThread
    private fun showToast(message: Int) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    @UiThread
    private fun setDownloadButtonState(app: App, enabled: Boolean) {
        val icon = if (enabled) R.drawable.ic_file_download_orange else R.drawable.ic_file_download_grey
        downloadButtons[app]?.setImageResource(icon)
    }

    @UiThread
    private fun setLoadAnimationState(visible: Boolean) {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = visible
    }
}