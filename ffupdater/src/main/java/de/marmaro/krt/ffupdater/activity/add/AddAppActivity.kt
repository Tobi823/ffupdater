package de.marmaro.krt.ffupdater.activity.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.marmaro.krt.ffupdater.R
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

        val items = mutableListOf<AddRecyclerView.ItemWrapper>()

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
            items.add(AddRecyclerView.WrappedTitle(titleText))

            val categoryApps = installedApps
                .filter { displayCategory in it.displayCategory }
                .filter { if (displayCategory == EOL) true else (EOL !in it.displayCategory) }
                .map { AddRecyclerView.WrappedApp(it.app) }
            items.addAll(categoryApps)
        }

        withContext(Dispatchers.Main) {
            val view = findViewById<RecyclerView>(R.id.add_app_activity__recycler_view)
            view.adapter = AddRecyclerView(items, this@AddAppActivity)
            view.layoutManager = LinearLayoutManager(this@AddAppActivity)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, AddAppActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

}