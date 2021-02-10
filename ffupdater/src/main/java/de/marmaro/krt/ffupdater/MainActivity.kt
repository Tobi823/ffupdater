package de.marmaro.krt.ffupdater

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Consumer
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.AppList
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.InstallAppDialog
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.SettingsHelper
import de.marmaro.krt.ffupdater.utils.OldDownloadsDeleter
import james.crasher.Crasher
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.BiConsumer
import java.util.function.Function

class MainActivity : AppCompatActivity() {
    private var swipeRefreshLayout: SwipeRefreshLayout = findViewById(R.id.swipeContainer)
    private var connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var helper = MainActivityHelper(this)
    private var crasher = Crasher(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        StrictModeSetup.enableStrictMode()
        AppCompatDelegate.setDefaultNightMode(SettingsHelper(this).getThemePreference(DeviceEnvironment()))
        Migrator().migrate(this)
        OldDownloadsDeleter.delete(this)

        // register listener
        for (app in AppList.values()) {
            helper.getInfoButtonForApp(app).setOnClickListener {
                AppInfoDialog(app.impl).show(supportFragmentManager)
            }
            helper.getDownloadButtonForApp(app).setOnClickListener {
                downloadApp(app)
            }
        }
        findViewById<View>(R.id.installAppButton).setOnClickListener {
            InstallAppDialog { app: AppList -> downloadApp(app) }.show(supportFragmentManager)
        }
        swipeRefreshLayout.setOnRefreshListener { updateUI(true) }
    }

    protected override fun onResume() {
        super.onResume()
        updateUI(false)
    }

    protected override fun onPause() {
        super.onPause()
        BackgroundUpdateCheckerCreator(this).startOrStopBackgroundUpdateCheck()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        metadataFetcher.shutdown()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_about) {
            val alertDialog = AlertDialog.Builder(this@MainActivity).create()
            alertDialog.setTitle(getString(R.string.action_about_title))
            alertDialog.setMessage(getString(R.string.infobox))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), DialogInterface.OnClickListener { dialog: DialogInterface, w: Int -> dialog.dismiss() })
            alertDialog.show()
        } else if (itemId == R.id.action_settings) { //start settings activity where we use select firefox product and release type;
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUI(crashOnException: Boolean) {
        if (isNetworkUnavailable) {
            Snackbar.make(findViewById<View>(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show()
            return
        }
        swipeRefreshLayout.setRefreshing(true)
        for (notInstalledApp in deviceAppRegister.getNotInstalledApps()) {
            helper!!.getAppCardViewForApp(notInstalledApp).visibility = View.GONE
        }
        val installedApps: List<App> = deviceAppRegister.getInstalledApps()
        for (app in installedApps) {
            val installedText: String = deviceAppRegister.getMetadata(app).map { metadata -> java.lang.String.format("Installed: %s", metadata.getVersionName()) }.orElse("Unknown version installed")
            helper!!.getAppCardViewForApp(app).visibility = View.VISIBLE
            helper!!.setInstalledVersionText(app, installedText)
            helper!!.setAvailableVersionText(app, getString(string.available_version_loading))
        }
        val futures: Map<App, Future<AvailableMetadata>> = metadataFetcher.fetchMetadata(installedApps)
        Thread {
            Thread.setDefaultUncaughtExceptionHandler(crasher)
            futures.forEach(BiConsumer<App, Future<AvailableMetadata>> { app: App, future: Future<AvailableMetadata> ->
                try {
                    updateUIForApp(app, future)
                } catch (e: InterruptedException) {
                    if (crashOnException) {
                        throw ParamRuntimeException(e, "failed to fetch available metadata")
                    }
                    Log.e(LOG_TAG, "failed to fetch available metadata", e)
                    runOnUiThread(Runnable {
                        helper!!.setAvailableVersionText(app, getString(R.string.available_version_error))
                        helper!!.disableDownloadButton(app)
                    })
                } catch (e: TimeoutException) {
                    if (crashOnException) {
                        throw ParamRuntimeException(e, "failed to fetch available metadata")
                    }
                    Log.e(LOG_TAG, "failed to fetch available metadata", e)
                    runOnUiThread(Runnable {
                        helper!!.setAvailableVersionText(app, getString(R.string.available_version_error))
                        helper!!.disableDownloadButton(app)
                    })
                } catch (e: ExecutionException) {
                    if (crashOnException) {
                        throw ParamRuntimeException(e, "failed to fetch available metadata")
                    }
                    Log.e(LOG_TAG, "failed to fetch available metadata", e)
                    runOnUiThread(Runnable {
                        helper!!.setAvailableVersionText(app, getString(R.string.available_version_error))
                        helper!!.disableDownloadButton(app)
                    })
                }
            })
            runOnUiThread(Runnable { swipeRefreshLayout.setRefreshing(false) })
        }.start()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    private fun updateUIForApp(app: App, future: Future<AvailableMetadata>) {
        val available: AvailableMetadata = future[30, TimeUnit.SECONDS]
        val installed: Optional<InstalledMetadata> = deviceAppRegister.getMetadata(app)
        val updateAvailable: Boolean = installed.map(Function<InstalledMetadata, Any> { metadata: InstalledMetadata? -> UpdateChecker().isUpdateAvailable(app, metadata, available) }
        ).orElse(true)
        val availableText: String
        if (app.getReleaseIdType() === App.ReleaseIdType.TIMESTAMP) {
            availableText = if (updateAvailable) getString(R.string.update_available) else getString(R.string.no_update_available)
        } else {
            availableText = getString(R.string.available_version,
                    available.getReleaseId().getValueAsString())
        }
        val installedText: String = installed.map(Function<InstalledMetadata, Any> { metadata: InstalledMetadata -> getString(R.string.installed_version, metadata.getVersionName()) }
        ).orElse(getString(R.string.unknown_installed_version))
        runOnUiThread(Runnable {
            helper!!.setInstalledVersionText(app, installedText)
            helper!!.setAvailableVersionText(app, availableText)
            if (updateAvailable) {
                helper!!.enableDownloadButton(app)
            } else {
                helper!!.disableDownloadButton(app)
            }
        })
    }

    private fun downloadApp(app: AppList) {
        if (isNetworkUnavailable) {
            Snackbar.make(findViewById<View>(R.id.coordinatorLayout), R.string.not_connected_to_internet, Snackbar.LENGTH_LONG).show()
            return
        }
        val intent = Intent(this, InstallActivity::class.java)
        intent.putExtra(InstallActivity.EXTRA_APP_NAME, app.name)
        startActivity(intent)
    }

    private val isNetworkUnavailable: Boolean
        private get() = Optional.ofNullable<NetworkInfo>(connectivityManager.getActiveNetworkInfo())
                .map<Boolean>(Function<NetworkInfo, Boolean> { info: NetworkInfo -> !info.isConnected() })
                .orElse(true)

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}