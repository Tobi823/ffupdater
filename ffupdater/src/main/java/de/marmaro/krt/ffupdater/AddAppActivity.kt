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
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.Category.*
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.AppWarningDialog
import de.marmaro.krt.ffupdater.security.StrictModeSetup
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper

class AddAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_app)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }
        StrictModeSetup.enableStrictMode()
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initUI()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @UiThread
    private fun initUI() {
        val mozillaBrowsers = findViewById<LinearLayout>(R.id.list_mozilla_browsers)
        mozillaBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.category == FROM_MOZILLA }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { initUIForApp(mozillaBrowsers, it) }

        val firefoxBasedBrowsers = findViewById<LinearLayout>(R.id.list_firefox_based_browsers)
        firefoxBasedBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.category == BASED_ON_FIREFOX }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { initUIForApp(firefoxBasedBrowsers, it) }

        val goodPrivacyBrowsers = findViewById<LinearLayout>(R.id.list_good_privacy_browsers)
        goodPrivacyBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.category == GOOD_PRIVACY_BROWSER }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { initUIForApp(goodPrivacyBrowsers, it) }

        val betterThanChromeBrowsers = findViewById<LinearLayout>(R.id.list_better_than_chrome_browsers)
        betterThanChromeBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.category == BETTER_THAN_GOOGLE_CHROME }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { initUIForApp(betterThanChromeBrowsers, it) }

        val eolBrowsers = findViewById<LinearLayout>(R.id.list_eol_browsers)
        eolBrowsers.removeAllViews()
        App.values()
            .filter { it.impl.category == EOL }
            .filter { it.impl.installableWithDefaultPermission }
            .filterNot { it.impl.isInstalled(this) }
            .sortedBy { getString(it.impl.title) }
            .forEach { initUIForApp(eolBrowsers, it) }
    }

    @UiThread
    private fun initUIForApp(mainLayout: LinearLayout, app: App) {
        val cardView = layoutInflater.inflate(R.layout.add_app_cardview, mainLayout, false)

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
            val intent = InstallActivity.createIntent(this, app)
            startActivity(intent)
            finish()
        }

        mainLayout.addView(cardView)
    }

    companion object {
        const val LOG_TAG = "AddAppActivity"
        fun createIntent(context: Context): Intent {
            return Intent(context, AddAppActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}