package de.marmaro.krt.ffupdater

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.app.impl.exceptions.NetworkException
import de.marmaro.krt.ffupdater.background.BackgroundJob
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.InstallNewAppDialog
import de.marmaro.krt.ffupdater.dialog.InstallSameVersionDialog
import de.marmaro.krt.ffupdater.dialog.RunningDownloadsDialog
import de.marmaro.krt.ffupdater.download.AppDownloadStatus.Companion.areDownloadsInBackgroundActive
import de.marmaro.krt.ffupdater.download.NetworkUtil.isNetworkMetered
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private val sameAppVersionAlreadyInstalled: EnumMap<App, Boolean> = EnumMap(App::class.java)
    private val availableVersions: EnumMap<App, TextView> = EnumMap(App::class.java)
    private val downloadButtons: EnumMap<App, ImageButton> = EnumMap(App::class.java)
    private val errorsDuringUpdateCheck: EnumMap<App, Exception?> = EnumMap(App::class.java)
    private lateinit var settingsHelper: SettingsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashListener.openCrashReporterForUncaughtExceptions(this)

        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (BuildConfig.BUILD_TYPE == "debug") {
            StrictModeSetup.enableStrictMode()
        }
        settingsHelper = SettingsHelper(this)
        AppCompatDelegate.setDefaultNightMode(settingsHelper.themePreference)
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
            BackgroundJob.startOrStopBackgroundUpdateCheck(this@MainActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        cleanUpObjects()
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
                Log.e("MainActivity", exception.toString())
                if (exception != null) {
                    val description =
                        getString(R.string.crash_report__explain_text__main_activity_update_check)
                    val intent = CrashReportActivity.createIntent(this, exception, description)
                    startActivity(intent)
                }
            }

            val downloadButton: ImageButton = newCardView.findViewWithTag("appDownloadButton")
            downloadButton.setOnClickListener {
                if (sameAppVersionAlreadyInstalled[app] == true) {
                    InstallSameVersionDialog.newInstance(app).show(supportFragmentManager)
                } else {
                    installApp(app, askForConfirmationIfOtherDownloadsAreRunning = true)
                }
            }
            downloadButtons[app] = downloadButton
            disableDownloadButton(app)

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
            .filter {
                it.detail.isInstalled(this@MainActivity)
            }

        val abortUpdateCheck = { message: Int ->
            apps.forEach { setAvailableVersion(it, getString(message)) }
            hideLoadAnimation()
            showToast(message)
        }
        if (!settingsHelper.isForegroundUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            abortUpdateCheck(R.string.main_activity__no_unmetered_network)
            return
        }

        showLoadAnimation()
        apps.forEach { setAvailableVersion(it, getString(R.string.available_version_loading)) }
        apps.forEach { checkForAppUpdateInIOThread(it) }
        hideLoadAnimation()
    }

    @MainThread
    private suspend fun checkForAppUpdateInIOThread(app: App) {
        try {
            val updateResult = app.detail.updateCheck(applicationContext)
            setAvailableVersion(app, updateResult.displayVersion)
            sameAppVersionAlreadyInstalled[app] = !updateResult.isUpdateAvailable
            if (updateResult.isUpdateAvailable) {
                enableDownloadButton(app)
            } else {
                disableDownloadButton(app)
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
        //it's possible that MainActivity is destroyed but the coroutine wants to update
        //the "available version" text field (which does not longer exists)
        availableVersions[app]?.text = message
        errorsDuringUpdateCheck[app] = null
    }

    @UiThread
    private fun showUpdateCheckError(app: App, message: Int, exception: Exception) {
        errorsDuringUpdateCheck[app] = exception
        availableVersions[app]?.setText(message)
        disableDownloadButton(app)
    }

    @MainThread
    fun installApp(app: App, askForConfirmationIfOtherDownloadsAreRunning: Boolean = false) {
        if (!settingsHelper.isForegroundUpdateCheckOnMeteredAllowed && isNetworkMetered(this)) {
            showToast(R.string.main_activity__no_unmetered_network)
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
    private fun enableDownloadButton(app: App) {
        downloadButtons[app]?.setImageResource(R.drawable.ic_file_download_orange)
    }

    @UiThread
    private fun disableDownloadButton(app: App) {
        downloadButtons[app]?.setImageResource(R.drawable.ic_file_download_grey)
    }

    @UiThread
    private fun showLoadAnimation() {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = true
    }

    @UiThread
    private fun hideLoadAnimation() {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = false
    }
}