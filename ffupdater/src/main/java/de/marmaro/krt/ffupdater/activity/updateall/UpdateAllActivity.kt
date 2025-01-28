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
import de.marmaro.krt.ffupdater.app.App
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

    private suspend fun startUpdate() {
        withContext(Dispatchers.IO) {
            val context = applicationContext
            val allApps = InstalledAppsCache.getAppsApplicableForBackgroundUpdate(context)
            val appStatus = allApps.map { it.findImpl().findStatusOrUseOldCache(context) } //
                .filter { it.isUpdateAvailable }
            if (appStatus.isEmpty()) {
                showText("No updates available for installation. Abort.")
                return@withContext
            }

            showText("These apps will be updated soon: " + appStatus.joinToString(",") { context.getString(it.app.findImpl().title) })
            for (status in appStatus) {
                updateApp(status.app, status.app.findImpl(), status)
            }
            showText("Done. All apps were updated (if possible). You can close the app now.")
        }
    }

    private suspend fun updateApp(app: App, appImpl: AppBase, status: InstalledAppStatus) {
        val title = applicationContext.getString(appImpl.title)
        showText("Start update installation for '$title'")
        val file = status.app.findImpl().getApkFile(applicationContext, status.latestVersion)
        if (!file.exists()) {
            showText("Downloaded file for '$title' does not exists, continue with the next app.")
            return
        }
        showText("Start installation of '$title'.")
        val result = try {
            installer.startInstallation(applicationContext, file, status.app.findImpl())
        } catch (e: InstallationFailedException) {
            showText("Installation of '$title' failed. Continue with the next app.")
            return
        }
        showText("Installation of '$title' successful. Certificate hash: ${result.certificateHash}")
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
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}