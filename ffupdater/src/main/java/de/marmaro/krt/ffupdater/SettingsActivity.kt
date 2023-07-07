package de.marmaro.krt.ffupdater

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.topjohnwu.superuser.Shell
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.crash.CrashListener
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.installer.entity.Installer
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.BackgroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.ForegroundSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper.DnsProvider.CUSTOM_SERVER
import rikka.shizuku.Shizuku
import java.time.Duration


/**
 * Activity for displaying the settings view.
 */
@Keep
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CrashListener.openCrashReporterForUncaughtExceptions(this)) {
            finish()
            return
        }
        setContentView(R.layout.activity_settings)
        AppCompatDelegate.setDefaultNightMode(ForegroundSettingsHelper.themePreference)
        if (savedInstanceState == null) { //https://stackoverflow.com/a/60348385
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPause() {
        super.onPause()
        BackgroundJob.initBackgroundUpdateCheck(applicationContext)
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

        private fun changeBackgroundUpdateCheck(
            enabled: Boolean?,
            interval: Duration?,
            onlyWhenIdle: Boolean?
        ): Boolean {
            BackgroundJob.changeBackgroundUpdateCheck(
                requireContext().applicationContext,
                enabled ?: BackgroundSettingsHelper.isUpdateCheckEnabled,
                interval ?: BackgroundSettingsHelper.updateCheckInterval,
                onlyWhenIdle ?: BackgroundSettingsHelper.isUpdateCheckOnlyAllowedWhenDeviceIsIdle
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

            findSwitchPref("device__prefer_32bit_apks").setOnPreferenceChangeListener { _, _ ->
                App.values().forEach {
                    it.downloadedFileCache.deleteAllApkFileForThisApp(requireContext())
                }
                true
            }

            findSwitchPref("network__trust_user_cas").setOnPreferenceChangeListener { _, _ ->
                FileDownloader.restart(requireContext().applicationContext)
                true
            }

            findListPref("network__dns_provider").setOnPreferenceChangeListener { _, newValue ->
                findTextPref("network__custom_doh_server").isVisible = (newValue == CUSTOM_SERVER.name)
                FileDownloader.restart(requireContext().applicationContext)
                true
            }

            findTextPref("network__custom_doh_server").isVisible =
                findListPref("network__dns_provider").value == CUSTOM_SERVER.name
            findTextPref("network__custom_doh_server").setOnPreferenceChangeListener { _, _ ->
                FileDownloader.restart(requireContext().applicationContext)
                true
            }

            findTextPref("network__proxy").setOnPreferenceChangeListener { _, _ ->
                FileDownloader.restart(requireContext().applicationContext)
                true
            }


            findListPref("installer__method").setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    Installer.ROOT_INSTALLER.name -> {
                        Shell.getShell().use {
                            if (it.isRoot) {
                                return@setOnPreferenceChangeListener true
                            }

                            val text = getString(R.string.installer__method__root_not_granted)
                            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                            return@setOnPreferenceChangeListener false
                        }
                    }
                    Installer.SHIZUKU_INSTALLER.name -> {
                        if (!DeviceSdkTester.INSTANCE.supportsAndroidMarshmallow()) {
                            val text = "Your Android is too old."
                            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                            return@setOnPreferenceChangeListener false
                        }
                        try {
                            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                                Shizuku.requestPermission(42)
                            }
                            return@setOnPreferenceChangeListener true
                        } catch (e: IllegalStateException) {
                            val text = getString(R.string.installer__method__shizuku_not_installed)
                            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                            return@setOnPreferenceChangeListener false
                        }
                    }
                    else -> true
                }
            }
        }
    }
}