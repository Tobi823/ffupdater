package de.marmaro.krt.ffupdater.activity.settings

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.Shell
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.background.BackgroundWork
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DataStoreHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettings
import de.marmaro.krt.ffupdater.settings.NetworkSettings.DnsProvider.CUSTOM_SERVER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku


/**
 * Activity for displaying the settings view.
 */
@Keep
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettings.themePreference)
        if (savedInstanceState == null) { //https://stackoverflow.com/a/60348385
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private fun findSwitchPref(key: String) = findPreference<SwitchPreferenceCompat>(key)!!
        private fun findListPref(key: String) = findPreference<ListPreference>(key)!!
        private fun findMultiPref(key: String) = findPreference<MultiSelectListPreference>(key)!!
        private fun findTextPref(key: String) = findPreference<EditTextPreference>(key)!!

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            hideOptionsForLowerApis()
            loadHiddenAppNames()
            loadExcludedAppNames()
            listenForBackgroundJobRestarts()
            listenForThemeChanges()
            deleteFileCacheWhenChange32BitAppsPreference()
            setupInstallerValidator()
            setupNetworkSettingsValidator()
        }

        private fun hideOptionsForLowerApis() {
            if (!DeviceSdkTester.supportsAndroid6M23()) {
                findSwitchPref("background__update_check__when_device_idle").summary =
                    getString(R.string.settings__background__update_check__when_device_idle__unsupported)
                findSwitchPref("background__update_check__when_device_idle").isEnabled = false
            }
        }

        private fun loadHiddenAppNames() {
            val hiddenApps = findMultiPref("foreground__hidden_apps")
            hiddenApps.entries = App.values()
                .map { getString(it.findImpl().title) }
                .toTypedArray()
            hiddenApps.entryValues = App.values()
                .map { it.name }
                .toTypedArray()
        }

        private fun loadExcludedAppNames() {
            val excludedApps = findMultiPref("background__update_check__excluded_apps")
            excludedApps.entries = App.values()
                .map { getString(it.findImpl().title) }
                .toTypedArray()
            excludedApps.entryValues = App.values()
                .map { it.name }
                .toTypedArray()
        }

        private fun listenForBackgroundJobRestarts() {
            val listener = Preference.OnPreferenceChangeListener { _, _ ->
                restartBackgroundJobAfterClosingActivity = true
                // reset to prevent false warning of non executed background update check
                DataStoreHelper.lastBackgroundCheck2 = 0
                true
            }
            findSwitchPref("background__update_check__enabled").onPreferenceChangeListener = listener
            findListPref("background__update_check__interval").onPreferenceChangeListener = listener
        }

        private fun listenForThemeChanges() {
            findListPref("foreground__theme_preference").setOnPreferenceChangeListener { _, newValue ->
                AppCompatDelegate.setDefaultNightMode((newValue as String).toInt())
                true
            }
        }

        private fun deleteFileCacheWhenChange32BitAppsPreference() {
            findSwitchPref("device__prefer_32bit_apks").setOnPreferenceChangeListener { _, _ ->
                lifecycleScope.launch(Dispatchers.Main) {
                    App.values().forEach {
                        it.findImpl().deleteFileCache(requireContext())
                    }
                }
                true
            }
        }

        private var restartBackgroundJobAfterClosingActivity = false

        private fun setupInstallerValidator() {
            findListPref("installer__method").setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    Installer.ROOT_INSTALLER.name -> canRootInstallerBeUsed()
                    Installer.SHIZUKU_INSTALLER.name -> canShizukuInstallerBeUsed()
                    else -> true
                }
            }
        }

        private fun canRootInstallerBeUsed(): Boolean {
            Shell.getShell().use {
                if (it.isRoot) {
                    return true
                }
            }
            showBriefMessage(R.string.installer__method__root_not_granted)
            return false
        }

        private fun canShizukuInstallerBeUsed(): Boolean {
            if (!DeviceSdkTester.supportsAndroid6M23()) {
                showBriefMessage(R.string.installer__android_too_old_for_shizuku)
                return false
            }
            return try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(42)
                }
                true
            } catch (e: IllegalStateException) {
                showBriefMessage(R.string.installer__method__shizuku_not_installed)
                false
            }
        }

        private fun setupNetworkSettingsValidator() {
            val listener = Preference.OnPreferenceChangeListener { _, _ ->
                FileDownloader.restart(requireContext().applicationContext)
                true
            }
            val dnsProvider = findListPref("network__dns_provider")
            val customDohServer = findTextPref("network__custom_doh_server")
            val trustUserCA = findSwitchPref("network__trust_user_cas")
            val networkProxy = findTextPref("network__proxy")

            trustUserCA.onPreferenceChangeListener = listener
            dnsProvider.setOnPreferenceChangeListener { pref, newValue ->
                customDohServer.isVisible = (newValue == CUSTOM_SERVER.name)
                listener.onPreferenceChange(pref, newValue)
            }
            customDohServer.isVisible = (dnsProvider.value == CUSTOM_SERVER.name)
            customDohServer.onPreferenceChangeListener = listener
            networkProxy.onPreferenceChangeListener = listener
        }

        @UiThread
        private fun showBriefMessage(message: Int) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
        }

        override fun onPause() {
            super.onPause()
            if (restartBackgroundJobAfterClosingActivity) {
                restartBackgroundJobAfterClosingActivity = false
                BackgroundWork.forceRestart(requireContext().applicationContext)
            }
        }
    }
}
