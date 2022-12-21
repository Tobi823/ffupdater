package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://f-droid.org/en/packages/us.spotco.fennec_dos/
 */
class MullFromRepo(
    private val customRepositoryConsumer: CustomRepositoryConsumer = CustomRepositoryConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.MULL_FROM_REPO
    override val codeName = "MullFromRepo"
    override val packageName = "us.spotco.fennec_dos"
    override val title = R.string.mull__title
    override val description = R.string.mull__description
    override val downloadSource = "https://divestos.org/fdroid/official"
    override val icon = R.mipmap.ic_logo_mull
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "260e0a49678c78b70c02d6537add3b6dc0a17171bbde8ce75fd4026a8a3e18d2"
    override val projectPage = "https://divestos.org/fdroid/official/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.i(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val abi = deviceAbiExtractor.findBestAbiForDeviceAndApp(supportedAbis, deviceSettings.prefer32BitApks)
        val result = customRepositoryConsumer.getLatestUpdate(
            settings = networkSettings,
            "https://divestos.org/fdroid/official",
            packageName,
            abi
        )
        Log.i(LOG_TAG, "found latest version ${result.version}")
        return result
    }

    companion object {
        private const val LOG_TAG = "MullFromRepo"
    }
}