package de.marmaro.krt.ffupdater

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import java.time.Duration


/**
 * Activity for displaying the settings view.
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }
        setContentView(R.layout.activity_settings)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper(this).themePreference)
        if (savedInstanceState == null) { //https://stackoverflow.com/a/60348385
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPause() {
        super.onPause()
        BackgroundJob.initBackgroundUpdateCheck(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private fun findSwitchPref(key: String) = findPreference<SwitchPreferenceCompat>(key)!!
        private fun findListPref(key: String) = findPreference<ListPreference>(key)!!
        private fun findMultiPref(key: String) = findPreference<MultiSelectListPreference>(key)!!
        private fun findTextPref(key: String) = findPreference<EditTextPreference>(key)!!

        private fun changeBackgroundUpdateCheck(
            enabled: Boolean?,
            interval: Duration?,
            onlyWhenIdle: Boolean?
        ): Boolean {
            val settings = BackgroundSettingsHelper(requireContext())
            BackgroundJob.changeBackgroundUpdateCheck(
                requireContext(),
                enabled ?: settings.isUpdateCheckEnabled,
                interval ?: settings.updateCheckInterval,
                onlyWhenIdle ?: settings.isUpdateCheckOnlyAllowedWhenDeviceIsIdle
            )
            return true
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            if (!DeviceSdkTester.INSTANCE.supportsAndroidMarshmallow()) {
                findSwitchPref("background__update_check__when_device_idle").summary =
                    getString(R.string.settings__background__update_check__when_device_idle__unsupported)
                findSwitchPref("background__update_check__when_device_idle").isEnabled = false
            }

            findListPref("foreground__theme_preference").setOnPreferenceChangeListener { _, newValue ->
                AppCompatDelegate.setDefaultNightMode((newValue as String).toInt())
                true
            }

            findSwitchPref("background__update_check__enabled").setOnPreferenceChangeListener { _, newValue ->
                changeBackgroundUpdateCheck(newValue as Boolean, null, null)
            }

            findListPref("background__update_check__interval").setOnPreferenceChangeListener { _, newValue ->
                changeBackgroundUpdateCheck(null, Duration.ofMinutes((newValue as String).toLong()), null)
            }

            findSwitchPref("background__update_check__when_device_idle").setOnPreferenceChangeListener { _, newValue ->
                changeBackgroundUpdateCheck(null, null, newValue as Boolean)
            }

            val excludedApps = findMultiPref("background__update_check__excluded_apps")
            excludedApps.entries = App.values()
                .map { app -> getString(app.impl.title) }
                .toTypedArray()
            excludedApps.entryValues = App.values()
                .map { app -> app.name }
                .toTypedArray()

            findSwitchPref("installer__session").setOnPreferenceChangeListener { _, newValue ->
                disableOtherInstallMethods(newValue as Boolean, null, null)
            }
            findSwitchPref("installer__native").setOnPreferenceChangeListener { _, newValue ->
                disableOtherInstallMethods(null, newValue as Boolean, null)
            }
            findSwitchPref("installer__root").setOnPreferenceChangeListener { _, newValue ->
                disableOtherInstallMethods(null, null, newValue as Boolean)
            }

            findSwitchPref("device__prefer_32bit_apks").setOnPreferenceChangeListener { _, _ ->
                App.values().forEach {
                    it.metadataCache.invalidateCache(requireContext())
                    it.downloadedFileCache.deleteApkFile(requireContext())
                }
                true
            }

            findTextPref("network__custom_doh_server").isVisible =
                findListPref("network__dns_provider").value == "5"
            findListPref("network__dns_provider").setOnPreferenceChangeListener { _, newValue ->
                findTextPref("network__custom_doh_server").isVisible = (newValue == "5")
                true
            }
        }

        private fun disableOtherInstallMethods(session: Boolean?, native: Boolean?, root: Boolean?): Boolean {
            if (native == true || root == true) {
                findSwitchPref("installer__session").isChecked = false
            }
            if (session == true || root == true) {
                findSwitchPref("installer__native").isChecked = false
            }
            if (session == true || native == true) {
                findSwitchPref("installer__root").isChecked = false
            }
            return true
        }
    }
}