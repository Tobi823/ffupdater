package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.*
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.AppWarningDialog
import de.marmaro.krt.ffupdater.dialog.RequestInstallationPermissionDialog
import de.marmaro.krt.ffupdater.dialog.RunningDownloadsDialog
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper

class AddAppActivity : AppCompatActivity() {
    private lateinit var foregroundSettings: ForegroundSettingsHelper
    private var finishActivityOnNextResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_app)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }
        StrictModeSetup.INSTANCE.enableStrictMode()
        foregroundSettings = ForegroundSettingsHelper(this)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addAppsToUserInterface()
    }

    override fun onResume() {
        super.onResume()
        if (finishActivityOnNextResume) {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @UiThread
    private fun addAppsToUserInterface() {
        val mozillaBrowsers = findViewById<LinearLayout>(R.id.list_mozilla_browsers)
        mozillaBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.displayCategory == FROM_MOZILLA }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { addAppToUserInterface(mozillaBrowsers, it) }

        val firefoxBasedBrowsers = findViewById<LinearLayout>(R.id.list_firefox_based_browsers)
        firefoxBasedBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.displayCategory == BASED_ON_FIREFOX }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { addAppToUserInterface(firefoxBasedBrowsers, it) }

        val goodPrivacyBrowsers = findViewById<LinearLayout>(R.id.list_good_privacy_browsers)
        goodPrivacyBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.displayCategory == GOOD_PRIVACY_BROWSER }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { addAppToUserInterface(goodPrivacyBrowsers, it) }

        val betterThanChromeBrowsers = findViewById<LinearLayout>(R.id.list_better_than_chrome_browsers)
        betterThanChromeBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.displayCategory == BETTER_THAN_GOOGLE_CHROME }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { addAppToUserInterface(betterThanChromeBrowsers, it) }

        val otherApplications = findViewById<LinearLayout>(R.id.other_applications)
        otherApplications.removeAllViews()
        App.values()
            .filter { it.impl.displayCategory == OTHER }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { addAppToUserInterface(otherApplications, it) }

        val eolBrowsers = findViewById<LinearLayout>(R.id.list_eol_browsers)
        eolBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.displayCategory == EOL }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { addAppToUserInterface(eolBrowsers, it) }
    }

    @UiThread
    private fun addAppToUserInterface(mainLayout: LinearLayout, app: App) {
        val cardView = layoutInflater.inflate(R.layout.activity_add_app_cardview, mainLayout, false)

        cardView.findViewWithTag<ImageView>("icon").setImageResource(app.impl.icon)
        cardView.findViewWithTag<TextView>("title").setText(app.impl.title)

        val warningButton = cardView.findViewWithTag<ImageButton>("warning_icon")
        if (app.impl.installationWarning == null) {
            warningButton.visibility = View.INVISIBLE
        }

        val eolReason = cardView.findViewWithTag<TextView>("eol_reason")
        if (app.impl.isEol()) {
            eolReason.text = getString(app.impl.eolReason!!)
        } else {
            eolReason.visibility = View.GONE
        }

        warningButton.setOnClickListener {
            AppWarningDialog.newInstance(app).show(supportFragmentManager)
        }
        cardView.findViewWithTag<ImageButton>("info_button").setOnClickListener {
            AppInfoDialog.newInstance(app).show(supportFragmentManager)
        }
        cardView.findViewWithTag<ImageButton>("open_project_page").setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.impl.projectPage))
            startActivity(browserIntent)
        }
        cardView.findViewWithTag<ImageButton>("add_app").setOnClickListener {
            installApp(app)
        }

        mainLayout.addView(cardView)
    }

    @MainThread
    private fun installApp(app: App) {
        if (!foregroundSettings.isUpdateCheckOnMeteredAllowed && NetworkUtil.isNetworkMetered(this)) {
            showToast(R.string.main_activity__no_unmetered_network)
            return
        }
        if (DeviceSdkTester.INSTANCE.supportsAndroidOreo() && !packageManager.canRequestPackageInstalls()) {
            RequestInstallationPermissionDialog().show(supportFragmentManager)
            return
        }
        if (FileDownloader.areDownloadsCurrentlyRunning()) {
            // this may updates the app
            RunningDownloadsDialog.newInstance(app, true).show(supportFragmentManager)
            return
        }
        val intent = DownloadActivity.createIntent(this, app)
        startActivity(intent)
        finish()
    }

    @UiThread
    private fun showToast(message: Int) {
        val layout = findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        const val LOG_TAG = "AddAppActivity"
        fun createIntent(context: Context): Intent {
            return Intent(context, AddAppActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}