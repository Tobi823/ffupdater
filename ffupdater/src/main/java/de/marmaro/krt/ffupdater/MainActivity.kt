package de.marmaro.krt.ffupdater

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
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
import de.marmaro.krt.ffupdater.app.impl.exceptions.ApiConsumerException
import de.marmaro.krt.ffupdater.app.impl.exceptions.GithubRateLimitExceededException
import de.marmaro.krt.ffupdater.background.BackgroundJob
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.InstallNewAppDialog
import de.marmaro.krt.ffupdater.dialog.InstallSameVersionDialog
import de.marmaro.krt.ffupdater.dialog.RunningDownloadsDialog
import de.marmaro.krt.ffupdater.download.DownloadManagerUtil
import de.marmaro.krt.ffupdater.download.NetworkUtil
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.PreferencesHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import kotlinx.coroutines.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.*

class MainActivity : AppCompatActivity() {
    private val sameAppVersionAlreadyInstalled: EnumMap<App, Boolean> = EnumMap(App::class.java)
    private val availableVersions: EnumMap<App, TextView> = EnumMap(App::class.java)
    private val downloadButtons: EnumMap<App, ImageButton> = EnumMap(App::class.java)
    private val errorsDuringUpdateCheck: EnumMap<App, Exception?> = EnumMap(App::class.java)
    private lateinit var downloadManager: DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashListener.openCrashReporterForUncaughtExceptions(this)

        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        StrictModeSetup.enableStrictMode()
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference())
        Migrator().migrate(this)
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        findViewById<View>(R.id.installAppButton).setOnClickListener {
            InstallNewAppDialog.newInstance().show(supportFragmentManager)
        }
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).setOnRefreshListener {
            lifecycleScope.launch(Dispatchers.Main) {
                checkForUpdates()
            }
        }
    }

    @MainThread
    private suspend fun userTriggersAppDownload(app: App) {
        if (NetworkUtil.isInternetUnavailable(this)) {
            showInternetUnavailableToast()
            return
        }
        // true: same version is already installed -> show warning
        // false: an other version is installed (probably older) ->
        //          install new version without warning
        // null: due to e.g. network issues it isn't known if the same version is installed ->
        //          install new version without warning
        if (sameAppVersionAlreadyInstalled[app] == true) {
            InstallSameVersionDialog.newInstance(app).show(supportFragmentManager)
        } else {
            installAppButCheckForCurrentDownloads(app)
        }
    }

    @MainThread
    suspend fun installAppButCheckForCurrentDownloads(app: App) {
        if (DownloadManagerUtil.isDownloadingAFileNow(downloadManager)) {
            RunningDownloadsDialog.newInstance(app).show(supportFragmentManager)
        } else {
            installApp(app)
        }
    }

    @MainThread
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.Main) {
            initUI()
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
            val timestamp = PreferencesHelper(this).lastBackgroundCheck
                ?.let { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(it) }
                ?: "/"
            AlertDialog.Builder(this@MainActivity)
                .setTitle(getString(R.string.action_about_title))
                .setMessage(getString(R.string.infobox, timestamp))
                .setNeutralButton(getString(R.string.ok))
                { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
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
    private suspend fun initUI() {
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
                lifecycleScope.launch(Dispatchers.Main) {
                    userTriggersAppDownload(app)
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
        // abort if layout is not initialized
//        if (findViewById<LinearLayout>(R.id.mainLinearLayout).childCount == 0) {
//            return
//        }
        val installedApps = App.values().filter { app ->
            app.detail.isInstalled(this@MainActivity)
        }
        if (NetworkUtil.isInternetUnavailable(this)) {
            installedApps.forEach { app ->
                val message = getString(R.string.main_activity__no_internet_connection)
                setAvailableVersion(app, message)
            }
            hideLoadAnimation()
            showInternetUnavailableToast()
            return
        }

        showLoadAnimation()
        installedApps.forEach { app ->
            setAvailableVersion(app, getString(R.string.available_version_loading))
        }
        installedApps.forEach { app ->
            checkForAppUpdateInIOThread(app)
        }
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
        } catch (e: ApiConsumerException) {
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
    suspend fun installApp(app: App) {
        if (NetworkUtil.isInternetUnavailable(this)) {
            showInternetUnavailableToast()
            return
        }
        val intent = Intent(this@MainActivity, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        startActivity(intent)
    }

    @UiThread
    private fun showInternetUnavailableToast() {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        val message = R.string.main_activity__no_internet_connection
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

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}