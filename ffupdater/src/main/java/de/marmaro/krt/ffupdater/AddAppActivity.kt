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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
import de.marmaro.krt.ffupdater.network.NetworkUtil.isNetworkMetered
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
        val installedApps = App.values()
            .filter { it.impl.installableByUser }
            .filter { DeviceAbiExtractor.INSTANCE.supportsOneOf(it.impl.supportedAbis) }
            .filter { !it.impl.isInstalledWithoutFingerprintVerification(applicationContext) }
            .sortedBy { getString(it.impl.title) }

        val items = mutableListOf<AvailableAppsAdapter.ItemWrapper>()

        for (displayCategory in DisplayCategory.values()) {
            val titleText = when (displayCategory) {
                FROM_MOZILLA -> getString(R.string.add_app_activity__title_from_mozilla)
                BASED_ON_FIREFOX -> getString(R.string.add_app_activity__title_from_mozilla)
                GOOD_PRIVACY_BROWSER -> getString(R.string.add_app_activity__title_good_privacy_browsers)
                BETTER_THAN_GOOGLE_CHROME -> getString(R.string.add_app_activity__title_better_than_google_chrome)
                OTHER -> getString(R.string.add_app_activity__title_other_applications)
                EOL -> getString(R.string.add_app_activity__title_end_of_live_browser)
            }
            items.add(AvailableAppsAdapter.WrappedTitle(titleText))

            val categoryApps = installedApps
                .filter { it.impl.displayCategory == displayCategory }
                .map { AvailableAppsAdapter.WrappedApp(it) }
            items.addAll(categoryApps)
        }

        val view = findViewById<RecyclerView>(R.id.add_app_activity__recycler_view)
        view.adapter = AvailableAppsAdapter(items, this)
        view.layoutManager = LinearLayoutManager(this)
    }

    companion object {
        const val LOG_TAG = "AddAppActivity"
        fun createIntent(context: Context): Intent {
            return Intent(context, AddAppActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    class AvailableAppsAdapter(
        private val elements: List<ItemWrapper>,
        private val activity: AppCompatActivity
    ) :
        RecyclerView.Adapter<ViewHolder>() {

        enum class ItemType(val id: Int) {
            APP(0), TITLE(1)
        }

        interface ItemWrapper {
            fun getType(): ItemType
        }

        class WrappedApp(val app: App) : ItemWrapper {
            override fun getType() = ItemType.APP
        }

        class WrappedTitle(val text: String) : ItemWrapper {
            override fun getType() = ItemType.TITLE
        }

        inner class AppHolder(itemView: View) : ViewHolder(itemView) {
            val title: TextView = itemView.findViewWithTag("title")
            val icon: ImageView = itemView.findViewWithTag("icon")
            val warningIcon: ImageButton = itemView.findViewWithTag("warning_icon")
            val eolReason: TextView = itemView.findViewWithTag("eol_reason")
            val infoButton: ImageButton = itemView.findViewWithTag("info_button")
            val openProjectPageButton: ImageButton = itemView.findViewWithTag("open_project_page")
            val addAppButton: ImageButton = itemView.findViewWithTag("add_app")
        }

        inner class HeadingHolder(itemView: View) : ViewHolder(itemView) {
            val text: TextView = itemView.findViewWithTag("text")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                ItemType.APP.id -> {
                    val appView = inflater.inflate(R.layout.activity_add_app_cardview, parent, false)
                    AppHolder(appView)
                }
                ItemType.TITLE.id -> {
                    val titleView = inflater.inflate(R.layout.activity_add_app_title, parent, false)
                    HeadingHolder(titleView)
                }
                else -> throw IllegalArgumentException()
            }
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                ItemType.APP.id -> onBindViewHolderApp(viewHolder as AppHolder, position)
                ItemType.TITLE.id -> onBindViewHolderTitle(viewHolder as HeadingHolder, position)
                else -> throw IllegalArgumentException()
            }
        }

        override fun getItemCount(): Int {
            return elements.size
        }

        override fun getItemViewType(position: Int): Int {
            return elements[position].getType().id
        }

        private fun onBindViewHolderApp(viewHolder: AppHolder, position: Int) {
            val wrappedApp = elements[position] as WrappedApp
            val app = wrappedApp.app
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

        private fun onBindViewHolderTitle(viewHolder: HeadingHolder, position: Int) {
            val heading = elements[position] as WrappedTitle
            viewHolder.text.text = heading.text
        }

        private fun installApp(app: App) {
            if (!ForegroundSettingsHelper(activity).isUpdateCheckOnMeteredAllowed && isNetworkMetered(activity)) {
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
    }
}