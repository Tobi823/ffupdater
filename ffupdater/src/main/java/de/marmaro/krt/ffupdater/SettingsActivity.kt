package de.marmaro.krt.ffupdater

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.crash.CrashListener
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
        setContentView(R.layout.settings_activity)
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
        private fun findSwitchPref(key: String) = findPreference<SwitchPreferenceCompat>(key)
        private fun findListPref(key: String) = findPreference<ListPreference>(key)

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findListPref("foreground__theme_preference")?.setOnPreferenceChangeListener { _, newValue ->
                AppCompatDelegate.setDefaultNightMode((newValue as String).toInt())
                true
            }

            findSwitchPref("background__update_check__enabled")?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                BackgroundJob.changeBackgroundUpdateCheck(requireContext(), enabled, null)
                true
            }

            findListPref("background__update_check__interval")?.setOnPreferenceChangeListener { _, newValue ->
                val minutes = (newValue as String).toLong()
                val duration = Duration.ofMinutes(minutes)
                BackgroundJob.changeBackgroundUpdateCheck(requireContext(), null, duration)
                true
            }

            val excludedApps =
                findPreference<MultiSelectListPreference>("background__update_check__excluded_apps")
            excludedApps?.entries = App.values()
                .map { app -> getString(app.impl.title) }
                .toTypedArray()
            excludedApps?.entryValues = App.values()
                .map { app -> app.name }
                .toTypedArray()

            val sessionInstaller = findSwitchPref("installer__session")
            val nativeInstaller = findSwitchPref("installer__native")
            val rootInstaller = findSwitchPref("installer__root")
            sessionInstaller?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    nativeInstaller?.isChecked = false
                    rootInstaller?.isChecked = false
                }
                true
            }
            nativeInstaller?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    sessionInstaller?.isChecked = false
                    rootInstaller?.isChecked = false
                }
                true
            }
            rootInstaller?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    sessionInstaller?.isChecked = false
                    nativeInstaller?.isChecked = false
                }
                true
            }
        }
    }
}