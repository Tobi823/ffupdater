package de.marmaro.krt.ffupdater.activity.updateall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.impl.AppBase
import de.marmaro.krt.ffupdater.device.InstalledAppsCache
import de.marmaro.krt.ffupdater.installer.AppInstaller
import de.marmaro.krt.ffupdater.installer.AppInstallerFactory
import de.marmaro.krt.ffupdater.installer.exceptions.InstallationFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Keep
class UpdateAllActivity : AppCompatActivity() {

    private lateinit var installer: AppInstaller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_updateall)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setOnApplyWindowInsetsListener(findViewById(R.id.updateall_activity__main_layout)) { v: View, insets: WindowInsetsCompat ->
            val bars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(leftMargin, topMargin + bars.top, rightMargin, bottomMargin + bars.bottom)
            }
            insets
        }

        installer = AppInstallerFactory.createForegroundAppInstaller(this)
        lifecycle.addObserver(installer)

        lifecycleScope.launch(Dispatchers.Main) {
            startUpdate()
        }
    }

    // exit if user clicks on the back-button on the top left (action bar)
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private suspend fun startUpdate() {
        withContext(Dispatchers.IO) {
            val context = applicationContext
            val allApps = InstalledAppsCache.getAppsApplicableForBackgroundUpdate(context)
            val appStatus = allApps.mapNotNull {
                try {
                    it.findImpl().findStatusOrUseOldCache(context)
                } catch (e: Exception) {
                    null // ignore app if it is not possible to find updates
                }
            }.filter { it.isUpdateAvailable }
            if (appStatus.isEmpty()) {
                showText(getString(R.string.update_all_activity__no_updates_available))
                return@withContext
            }

            val appStr = appStatus.joinToString(",") { context.getString(it.app.findImpl().title) }
            showText(getString(R.string.update_all_activity__these_apps_will_be_updated, appStr))
            for (status in appStatus) {
                updateApp(status.app.findImpl(), status)
            }
            showText(getString(R.string.update_all_activity__all_apps_updated))
        }
    }

    private suspend fun updateApp(appImpl: AppBase, status: InstalledAppStatus) {
        val title = applicationContext.getString(appImpl.title)
        showText(getString(R.string.update_all_activity__start_update, title))
        val file = status.app.findImpl().getApkFile(applicationContext, status.latestVersion)
        if (!file.exists()) {
            showText(getString(R.string.update_all_activity__downloaded_file_is_missing, title))
            return
        }
        showText(getString(R.string.update_all_activity__start_installation, title))
        val result = try {
            withContext(Dispatchers.Main) {
                installer.startInstallation(this@UpdateAllActivity, file, status.app.findImpl())
            }
        } catch (e: InstallationFailedException) {
            showText(getString(R.string.update_all_activity__installation_failed, title))
            return
        }
        showText(getString(R.string.update_all_activity__installation_successful, title, result.certificateHash))
    }

    private suspend fun showText(str: String) {
        withContext(Dispatchers.Main) {
            val entry = TextView(applicationContext)
            entry.text = str

            val layout = findViewById<LinearLayout>(R.id.updateall_activity__main_layout)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            entry.layoutParams = params
            layout.addView(entry)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, UpdateAllActivity::class.java)
        }
    }
}