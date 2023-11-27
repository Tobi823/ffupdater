package de.marmaro.krt.ffupdater

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BASED_ON_FIREFOX
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BETTER_THAN_GOOGLE_CHROME
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.EOL
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.FROM_MOZILLA
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_PRIVACY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_SECURITY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.OTHER
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.values
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.dialog.AppInstallationInfoDialog
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Keep
class AddAppActivity : AppCompatActivity() {
    private var finishActivityOnNextResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_app)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch(Dispatchers.IO) {
            addAppsToUserInterface()
        }
    }

    override fun onResume() {
        super.onResume()
        if (finishActivityOnNextResume) {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @UiThread
    private suspend fun addAppsToUserInterface() {
        val installedApps = App.values()
            .map { it.findImpl() }
            .filter { it.installableByUser }
            .filter { DeviceAbiExtractor.supportsOneOf(it.supportedAbis) }
            .filter { !it.isInstalledWithoutFingerprintVerification(applicationContext.packageManager) }

        val items = mutableListOf<AvailableAppsAdapter.ItemWrapper>()

        for (displayCategory in values()) {
            val titleText = when (displayCategory) {
                FROM_MOZILLA -> getString(R.string.add_app_activity__title_from_mozilla)
                BASED_ON_FIREFOX -> getString(R.string.add_app_activity__title_based_on_firefox)
                GOOD_PRIVACY_BROWSER -> getString(R.string.add_app_activity__title_good_privacy_browsers)
                GOOD_SECURITY_BROWSER -> getString(R.string.add_app_activity__title_good_security_browsers)
                BETTER_THAN_GOOGLE_CHROME -> getString(R.string.add_app_activity__title_better_than_google_chrome)
                OTHER -> getString(R.string.add_app_activity__title_other_applications)
                EOL -> getString(R.string.add_app_activity__title_end_of_live_browser)
            }
            items.add(AvailableAppsAdapter.WrappedTitle(titleText))

            val categoryApps = installedApps
                .filter { displayCategory in it.displayCategory }
                .filter { if (displayCategory == EOL) true else (EOL !in it.displayCategory) }
                .map { AvailableAppsAdapter.WrappedApp(it.app) }
            items.addAll(categoryApps)
        }

        withContext(Dispatchers.Main) {
            val view = findViewById<RecyclerView>(R.id.add_app_activity__recycler_view)
            view.adapter = AvailableAppsAdapter(items, this@AddAppActivity)
            view.layoutManager = LinearLayoutManager(this@AddAppActivity)
        }
    }

    companion object {
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
            val appImpl = app.findImpl()
            viewHolder.title.setText(appImpl.title)
            viewHolder.icon.setImageResource(appImpl.icon)
            viewHolder.addAppButton.setOnClickListener {
                AppInstallationInfoDialog(app).show(activity.supportFragmentManager)
            }
        }

        private fun onBindViewHolderTitle(viewHolder: HeadingHolder, position: Int) {
            val heading = elements[position] as WrappedTitle
            viewHolder.text.text = heading.text
        }

        @UiThread
        private fun showToast(message: Int) {
            val layout = activity.findViewById<View>(R.id.coordinatorLayout)
            Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
        }
    }
}