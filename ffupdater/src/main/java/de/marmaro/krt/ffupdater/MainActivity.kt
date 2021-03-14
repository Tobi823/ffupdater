package de.marmaro.krt.ffupdater

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog
import de.marmaro.krt.ffupdater.download.InternetConnectionTester
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
    private lateinit var cm: ConnectivityManager
    private val deviceEnvironment = DeviceEnvironment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Crasher(this)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        StrictModeSetup.enableStrictMode(deviceEnvironment)
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(deviceEnvironment))
        Migrator().migrate(this)
        OldDownloadsDeleter.delete(this)
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        for (app in App.values()) {
            getInfoButtonForApp(app).setOnClickListener {
                AppInfoDialog.newInstance(app).show(supportFragmentManager)
            }
            getDownloadButtonForApp(app).setOnClickListener {
                downloadApp(app)
            }
        }
        findViewById<View>(R.id.installAppButton).setOnClickListener {
            InstallAppDialog.newInstance().show(supportFragmentManager)
        }
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).setOnRefreshListener {
            updateUI(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI(false)
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

    private fun updateUI(crashOnException: Boolean) {
        val internetAvailable = InternetConnectionTester.isInternetAvailable(cm, deviceEnvironment)
        if (!internetAvailable) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show()
        }
        findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = true
        val jobs = ConcurrentLinkedQueue<Job>()
        for (app in App.values()) {
            if (app.detail.isInstalled(this)) {
                getAppCardViewForApp(app).visibility = View.VISIBLE
                getInstalledVersionTextView(app).text = app.detail.getDisplayInstalledVersion(this)
                if (internetAvailable) {
                    getAvailableVersionTextView(app).text = getString(R.string.available_version_loading)
                    jobs.add(updateUIForApp(app, crashOnException))
                } else {
                    getAvailableVersionTextView(app).text = getString(R.string.not_connected_to_internet)
                }
            } else {
                getAppCardViewForApp(app).visibility = View.GONE
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            jobs.forEach { it.join() }
            lifecycleScope.launch(Dispatchers.Main) {
                findViewById<SwipeRefreshLayout>(R.id.swipeContainer).isRefreshing = false
            }
        }
    }

    private fun updateUIForApp(app: App, crashOnException: Boolean): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = app.detail.updateCheck(applicationContext, deviceEnvironment)
                lifecycleScope.launch(Dispatchers.Main) {
                    getAvailableVersionTextView(app).text = result.displayVersion
                    if (result.isUpdateAvailable) {
                        enableDownloadButton(app)
                    } else {
                        disableDownloadButton(app)
                    }
                }
            } catch (e: Exception) {
                if (crashOnException) {
                    throw UpdateCheckException("fail to check $app for updates", e)
                }
                Log.e(LOG_TAG, "fail to check $app for updates", e)
                lifecycleScope.launch(Dispatchers.Main) {
                    getAvailableVersionTextView(app).text = getString(R.string.available_version_error)
                    disableDownloadButton(app)
                }
            }
        }
    }

    /**
     * Will be called by the dialogs like InstallAppDialog.
     */
    fun downloadApp(app: App) {
        if (InternetConnectionTester.isInternetUnavailable(cm, deviceEnvironment)) {
            Snackbar.make(findViewById(R.id.coordinatorLayout),
                    R.string.not_connected_to_internet,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }
        val intent = Intent(this, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        startActivity(intent)
    }

    private fun getAppCardViewForApp(app: App): CardView {
        return findViewById(when (app) {
            App.FIREFOX_KLAR -> R.id.firefoxKlarCard
            App.FIREFOX_FOCUS -> R.id.firefoxFocusCard
            App.FIREFOX_LITE -> R.id.firefoxLiteCard
            App.FIREFOX_RELEASE -> R.id.firefoxReleaseCard
            App.FIREFOX_BETA -> R.id.firefoxBetaCard
            App.FIREFOX_NIGHTLY -> R.id.firefoxNightlyCard
            App.LOCKWISE -> R.id.lockwiseCard
            App.BRAVE -> R.id.braveCard
            App.ICERAVEN -> R.id.iceravenCard
        })
    }

    private fun enableDownloadButton(app: App) {
        getDownloadButtonForApp(app).setImageResource(R.drawable.ic_file_download_orange)
    }

    private fun disableDownloadButton(app: App) {
        getDownloadButtonForApp(app).setImageResource(R.drawable.ic_file_download_grey)
    }

    private fun getDownloadButtonForApp(app: App): ImageButton {
        return findViewById(when (app) {
            App.FIREFOX_KLAR -> R.id.firefoxKlarDownloadButton
            App.FIREFOX_FOCUS -> R.id.firefoxFocusDownloadButton
            App.FIREFOX_LITE -> R.id.firefoxLiteDownloadButton
            App.FIREFOX_RELEASE -> R.id.firefoxReleaseDownloadButton
            App.FIREFOX_BETA -> R.id.firefoxBetaDownloadButton
            App.FIREFOX_NIGHTLY -> R.id.firefoxNightlyDownloadButton
            App.LOCKWISE -> R.id.lockwiseDownloadButton
            App.BRAVE -> R.id.braveDownloadButton
            App.ICERAVEN -> R.id.iceravenDownloadButton
        })
    }

    private fun getInstalledVersionTextView(app: App): TextView {
        return findViewById(when (app) {
            App.FIREFOX_KLAR -> R.id.firefoxKlarInstalledVersion
            App.FIREFOX_FOCUS -> R.id.firefoxFocusInstalledVersion
            App.FIREFOX_LITE -> R.id.firefoxLiteInstalledVersion
            App.FIREFOX_RELEASE -> R.id.firefoxReleaseInstalledVersion
            App.FIREFOX_BETA -> R.id.firefoxBetaInstalledVersion
            App.FIREFOX_NIGHTLY -> R.id.firefoxNightlyInstalledVersion
            App.LOCKWISE -> R.id.lockwiseInstalledVersion
            App.BRAVE -> R.id.braveInstalledVersion
            App.ICERAVEN -> R.id.iceravenInstalledVersion
        })
    }

    private fun getAvailableVersionTextView(app: App): TextView {
        return findViewById(when (app) {
            App.FIREFOX_KLAR -> R.id.firefoxKlarAvailableVersion
            App.FIREFOX_FOCUS -> R.id.firefoxFocusAvailableVersion
            App.FIREFOX_LITE -> R.id.firefoxLiteAvailableVersion
            App.FIREFOX_RELEASE -> R.id.firefoxReleaseAvailableVersion
            App.FIREFOX_BETA -> R.id.firefoxBetaAvailableVersion
            App.FIREFOX_NIGHTLY -> R.id.firefoxNightlyAvailableVersion
            App.LOCKWISE -> R.id.lockwiseAvailableVersion
            App.BRAVE -> R.id.braveAvailableVersion
            App.ICERAVEN -> R.id.iceravenAvailableVersion
        })
    }

    private fun getInfoButtonForApp(app: App): View {
        return findViewById(when (app) {
            App.FIREFOX_KLAR -> R.id.firefoxKlarInfoButton
            App.FIREFOX_FOCUS -> R.id.firefoxFocusInfoButton
            App.FIREFOX_LITE -> R.id.firefoxLiteInfoButton
            App.FIREFOX_RELEASE -> R.id.firefoxReleaseInfoButton
            App.FIREFOX_BETA -> R.id.firefoxBetaInfoButton
            App.FIREFOX_NIGHTLY -> R.id.firefoxNightlyInfoButton
            App.LOCKWISE -> R.id.lockwiseInfoButton
            App.BRAVE -> R.id.braveInfoButton
            App.ICERAVEN -> R.id.iceravenInfoButton
        })
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    private class UpdateCheckException(message: String, throwable: Throwable) : Exception(message, throwable)
}