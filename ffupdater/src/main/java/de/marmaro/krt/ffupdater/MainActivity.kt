package de.marmaro.krt.ffupdater

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog
import de.marmaro.krt.ffupdater.notification.BackgroundUpdateChecker
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import de.marmaro.krt.ffupdater.utils.OldDownloadsDeleter
import james.crasher.Crasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.*

class MainActivity : AppCompatActivity() {
    private var swipeRefreshLayout: SwipeRefreshLayout = findViewById(R.id.swipeContainer)
    private var connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var helper = MainActivityHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Crasher(this)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        StrictModeSetup.enableStrictMode()
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(DeviceEnvironment()))
        Migrator().migrate(this)
        OldDownloadsDeleter.delete(this)

        for (app in App.values()) {
            helper.getInfoButtonForApp(app).setOnClickListener {
                AppInfoDialog(app.detail).show(supportFragmentManager)
            }
            helper.getDownloadButtonForApp(app).setOnClickListener {
                downloadApp(app)
            }
        }
        findViewById<View>(R.id.installAppButton).setOnClickListener {
            InstallAppDialog { app: App -> downloadApp(app) }.show(supportFragmentManager)
        }
        swipeRefreshLayout.setOnRefreshListener { updateUI(true) }
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
            val alertDialog = AlertDialog.Builder(this@MainActivity).create()
            alertDialog.setTitle(getString(R.string.action_about_title))
            alertDialog.setMessage(getString(R.string.infobox))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok))
            { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            alertDialog.show()
        } else if (itemId == R.id.action_settings) {
            //start settings activity where we use select firefox product and release type;
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUI(crashOnException: Boolean) {
        if (isNetworkUnavailable()) {
            Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show()
            return
        }
        swipeRefreshLayout.isRefreshing = true
        val deviceEnvironment = DeviceEnvironment()
        val jobs = ConcurrentLinkedQueue<Job>()
        for (app in App.values()) {
            if (app.detail.isInstalled(this)) {
                helper.getAppCardViewForApp(app).visibility = View.VISIBLE
                helper.getInstalledVersionTextView(app).text = app.detail.getDisplayInstalledVersion(this)
                helper.getAvailableVersionTextView(app).text = getString(R.string.available_version_loading)
                jobs.add(updateUIForApp(app, deviceEnvironment, crashOnException))
            } else {
                helper.getAppCardViewForApp(app).visibility = View.GONE
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            jobs.forEach { it.join() }
            lifecycleScope.launch(Dispatchers.Main) { swipeRefreshLayout.isRefreshing = false }
        }
    }

    private fun updateUIForApp(app: App, deviceEnvironment: DeviceEnvironment, crashOnException: Boolean): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = app.detail.updateCheckAsync(applicationContext, deviceEnvironment).await()
                lifecycleScope.launch(Dispatchers.Main) {
                    helper.getAvailableVersionTextView(app).text = result.displayVersion
                    if (result.isUpdateAvailable) {
                        helper.enableDownloadButton(app)
                    } else {
                        helper.disableDownloadButton(app)
                    }
                }
            } catch (e: Exception) {
                if (crashOnException) {
                    throw UpdateCheckException("fail to check $app for updates", e)
                }
                Log.e(LOG_TAG, "fail to check $app for updates", e)
                lifecycleScope.launch(Dispatchers.Main) {
                    helper.getAvailableVersionTextView(app).text = getString(R.string.available_version_error)
                    helper.disableDownloadButton(app)
                }
            }
        }
    }

    private fun downloadApp(app: App) {
        if (isNetworkUnavailable()) {
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

    private fun isNetworkUnavailable(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // https://gist.github.com/Farbklex/f84029889444ee9c52a331a7e2bd10d2
            val activeNetwork = connectivityManager.activeNetwork ?: return true
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    ?: return true
            return capabilities.hasCapability(NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NET_CAPABILITY_VALIDATED)
        }
        return connectivityManager.getActiveNetworkInfo()?.isConnected != true
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }

    private class UpdateCheckException(message: String, throwable: Throwable) : Exception(message, throwable)
}