package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.*
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.dialog.AppInfoDialog
import de.marmaro.krt.ffupdater.dialog.AppWarningDialog
import de.marmaro.krt.ffupdater.dialog.RequestInstallationPermissionDialog
import de.marmaro.krt.ffupdater.dialog.RunningDownloadsDialog
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.NetworkUtil
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        foregroundSettings = ForegroundSettingsHelper(this)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch(Dispatchers.Main) {
            addAppsToUserInterface()
        }
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
    private suspend fun addAppsToUserInterface() {
        val installedApps = App.values()
            .filter { it.impl.installableByUser }
            .filter { DeviceAbiExtractor.INSTANCE.supportsOneOf(it.impl.supportedAbis) }
            .filter { !it.impl.isInstalledWithoutFingerprintVerification(applicationContext) }
            .sortedBy { getString(it.impl.title) }

        addCategoryToUi(installedApps, FROM_MOZILLA, R.id.list_mozilla_browsers)
        addCategoryToUi(installedApps, BASED_ON_FIREFOX, R.id.list_firefox_based_browsers)
        addCategoryToUi(installedApps, GOOD_PRIVACY_BROWSER, R.id.list_good_privacy_browsers)
        addCategoryToUi(installedApps, BETTER_THAN_GOOGLE_CHROME, R.id.list_better_than_chrome_browsers)
        addCategoryToUi(installedApps, OTHER, R.id.other_applications)
        addCategoryToUi(installedApps, EOL, R.id.list_eol_browsers)
    }

    private fun addCategoryToUi(apps: List<App>, displayCategory: DisplayCategory, id: Int) {
        val categoryApps = apps.filter { it.impl.displayCategory == displayCategory }
        val view = findViewById<RecyclerView>(id)
        view.adapter = AppsAdapter(categoryApps, this)
        view.layoutManager = LinearLayoutManager(this)
    }

    companion object {
        const val LOG_TAG = "AddAppActivity"
        fun createIntent(context: Context): Intent {
            return Intent(context, AddAppActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    class AppsAdapter(private val apps: List<App>, private val activity: AppCompatActivity) :
        RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        // Provide a direct reference to each of the views within a data item
        // Used to cache the views within the item layout for fast access
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            // Your holder should contain and initialize a member variable
            // for any view that will be set as you render a row
            val title: TextView = itemView.findViewWithTag<TextView>("title")
            val icon: ImageView = itemView.findViewWithTag<ImageView>("icon")
            val warningIcon: ImageButton = itemView.findViewWithTag<ImageButton>("warning_icon")
            val eolReason: TextView = itemView.findViewWithTag<TextView>("eol_reason")
            val infoButton: ImageButton = itemView.findViewWithTag<ImageButton>("info_button")
            val openProjectPageButton: ImageButton =
                itemView.findViewWithTag<ImageButton>("open_project_page")
            val addAppButton: ImageButton = itemView.findViewWithTag<ImageButton>("add_app")
        }

        // ... constructor and member variables
        // Usually involves inflating a layout from XML and returning the holder
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsAdapter.ViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            // Inflate the custom layout
            val contactView = inflater.inflate(R.layout.activity_add_app_cardview, parent, false)
            // Return a new holder instance
            return ViewHolder(contactView)
        }

        // Involves populating data into the item through holder
        override fun onBindViewHolder(viewHolder: AppsAdapter.ViewHolder, position: Int) {
            val app = apps[position]
            viewHolder.title.setText(app.impl.title)
            viewHolder.icon.setImageResource(app.impl.icon)

            val warning = app.impl.installationWarning != null
            viewHolder.warningIcon.visibility = if (warning) View.INVISIBLE else View.VISIBLE

            viewHolder.eolReason.visibility = if (app.impl.isEol()) View.VISIBLE else View.GONE
            app.impl.eolReason?.let { viewHolder.eolReason.setText(it) }

            viewHolder.warningIcon.setOnClickListener {
                AppWarningDialog.newInstance(app).show(activity.supportFragmentManager)
            }

            viewHolder.infoButton.setOnClickListener {
                AppInfoDialog.newInstance(app).show(activity.supportFragmentManager)
            }

            viewHolder.openProjectPageButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.impl.projectPage))
                activity.startActivity(browserIntent)
            }

            viewHolder.addAppButton.setOnClickListener {
                installApp(app)
            }
        }

        private fun installApp(app: App) {
            if (!ForegroundSettingsHelper(activity).isUpdateCheckOnMeteredAllowed &&
                NetworkUtil.isNetworkMetered(activity)
            ) {
                showToast(R.string.main_activity__no_unmetered_network)
                return
            }
            if (DeviceSdkTester.INSTANCE.supportsAndroidOreo() && !activity.packageManager.canRequestPackageInstalls()) {
                RequestInstallationPermissionDialog().show(activity.supportFragmentManager)
                return
            }
            if (FileDownloader.areDownloadsCurrentlyRunning()) {
                // this may updates the app
                RunningDownloadsDialog.newInstance(app, true).show(activity.supportFragmentManager)
                return
            }
            val intent = DownloadActivity.createIntent(activity, app)
            activity.startActivity(intent)
            activity.finish()
        }

        @UiThread
        private fun showToast(message: Int) {
            val layout = activity.findViewById<View>(R.id.coordinatorLayout)
            Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
        }

        // Returns the total count of items in the list
        override fun getItemCount(): Int {
            return apps.size
        }
    }
}