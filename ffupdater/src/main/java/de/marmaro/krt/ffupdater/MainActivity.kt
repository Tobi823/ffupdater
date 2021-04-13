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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.InstallNewAppDialog
import de.marmaro.krt.ffupdater.dialog.InstallSameVersionDialog
import de.marmaro.krt.ffupdater.download.NetworkTester
import de.marmaro.krt.ffupdater.notification.BackgroundUpdateChecker
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.PreferencesHelper
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import de.marmaro.krt.ffupdater.utils.OldDownloadsDeleter
import james.crasher.Crasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.*

class MainActivity : AppCompatActivity() {
    private val deviceEnvironment = DeviceEnvironment()
    private val sameAppVersionAlreadyInstalled: EnumMap<App, Boolean> = EnumMap(App::class.java)
    private val availableVersions: EnumMap<App, TextView> = EnumMap(App::class.java)
    private val downloadButtons: EnumMap<App, ImageButton> = EnumMap(App::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Crasher(this)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        StrictModeSetup.enableStrictMode(deviceEnvironment)
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(deviceEnvironment))
        Migrator().migrate(this)
        OldDownloadsDeleter.delete(this)

        findViewById<View>(R.id.installAppButton).setOnClickListener {
            InstallNewAppDialog.newInstance().show(supportFragmentManager)
        }
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).setOnRefreshListener {
            checkForUpdates()
        }
    }

    private fun userTriggersAppDownload(app: App) {
        if (NetworkTester.isInternetUnavailable(this)) {
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
            installApp(app)
        }
    }

    override fun onResume() {
        super.onResume()
        initUI()
        checkForUpdates(true)
    }

    override fun onPause() {
        super.onPause()
        BackgroundUpdateChecker.startOrStopBackgroundUpdateCheck(this)
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

    private fun initUI() {
        val mainLinearLayout = findViewById<LinearLayout>(R.id.mainLinearLayout)
        mainLinearLayout.removeAllViews()
        availableVersions.clear()
        downloadButtons.clear()

        val installedApps = App.values().filter { it.detail.isInstalled(this) }
        installedApps.forEach { app ->
            val newCardView = layoutInflater.inflate(R.layout.app_card_layout, mainLinearLayout, false)

            val installedVersion = newCardView.findViewWithTag<TextView>("appInstalledVersion")
            installedVersion.text = app.detail.getDisplayInstalledVersion(this)

            availableVersions[app] = newCardView.findViewWithTag("appAvailableVersion")

            val downloadButton = newCardView.findViewWithTag<ImageButton>("appDownloadButton")
            downloadButton.setOnClickListener { userTriggersAppDownload(app) }
            downloadButtons[app] = downloadButton
            disableDownloadButton(app)

            val infoButton = newCardView.findViewWithTag<ImageButton>("appInfoButton")
            infoButton.setOnClickListener {
                AppInfoDialog.newInstance(app).show(supportFragmentManager)
            }

            val title = getString(app.detail.displayTitle)
            newCardView.findViewWithTag<TextView>("appCardTitle").text = title

            val icon = newCardView.findViewWithTag<ImageView>("appIcon")
            icon.setImageResource(app.detail.displayIcon)
            icon.setBackgroundColor(app.detail.displayIconBackground)

            mainLinearLayout.addView(newCardView)
        }
    }

    private fun checkForUpdates(ignoreErrors: Boolean = false) {
        // abort if layout is not initialized
        if (findViewById<LinearLayout>(R.id.mainLinearLayout).childCount == 0) {
            return
        }

        val installedApps = App.values().filter { it.detail.isInstalled(this) }
        if (NetworkTester.isInternetUnavailable(this)) {
            val errorMessage = getString(R.string.main_activity__not_connected_to_internet)
            installedApps.forEach { availableVersions[it]!!.text = errorMessage }
            hideLoadAnimation()
            showInternetUnavailableToast()
            return
        }

        showLoadAnimation()
        val jobs = ConcurrentLinkedQueue<Job>()
        installedApps.forEach {
            availableVersions[it]!!.text = getString(R.string.available_version_loading)
            jobs.add(checkForAppUpdate(it, ignoreErrors))
        }
        lifecycleScope.launch(Dispatchers.IO) {
            jobs.forEach { it.join() }
            lifecycleScope.launch(Dispatchers.Main) {
                hideLoadAnimation()
            }
        }
    }

    private fun checkForAppUpdate(app: App, ignoreErrors: Boolean): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updateResult = app.detail.updateCheck(applicationContext, deviceEnvironment)
                lifecycleScope.launch(Dispatchers.Main) {
                    availableVersions[app]!!.text = updateResult.displayVersion
                    if (updateResult.isUpdateAvailable) {
                        sameAppVersionAlreadyInstalled[app] = false
                        enableDownloadButton(app)
                    } else {
                        sameAppVersionAlreadyInstalled[app] = true
                        disableDownloadButton(app)
                    }
                }
            } catch (e: Exception) {
                if (!ignoreErrors) {
                    throw UpdateCheckException("fail to check $app for updates", e)
                }
                Log.e(LOG_TAG, "fail to check $app for updates", e)
                lifecycleScope.launch(Dispatchers.Main) {
                    availableVersions[app]!!.text = getString(R.string.available_version_error)
                    disableDownloadButton(app)
                }
            }
        }
    }

    fun installApp(app: App) {
        if (NetworkTester.isInternetUnavailable(this)) {
            showInternetUnavailableToast()
            return
        }
        val intent = Intent(this, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        startActivity(intent)
    }

    private fun showInternetUnavailableToast() {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, R.string.main_activity__not_connected_to_internet, Snackbar.LENGTH_LONG).show()
    }

    private fun enableDownloadButton(app: App) {
        downloadButtons[app]!!.setImageResource(R.drawable.ic_file_download_orange)
    }

    private fun disableDownloadButton(app: App) {
        downloadButtons[app]!!.setImageResource(R.drawable.ic_file_download_grey)
    }

    private fun showLoadAnimation() {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = true
    }

    private fun hideLoadAnimation() {
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = false
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    private class UpdateCheckException(message: String, throwable: Throwable) : Exception(message, throwable)
}