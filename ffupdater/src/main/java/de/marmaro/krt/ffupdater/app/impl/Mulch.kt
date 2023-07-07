package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

@Keep
class Mulch : AppBase() {
    override val app = App.MULCH
    override val packageName = "us.spotco.mulch"
    override val title = R.string.mulch__title
    override val description = R.string.mulch__description
    override val installationWarning = R.string.mulch__warning
    override val downloadSource = "https://divestos.org/fdroid/official"
    override val icon = R.drawable.ic_logo_mulch
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "260e0a49678c78b70c02d6537add3b6dc0a17171bbde8ce75fd4026a8a3e18d2"
    override val projectPage = "https://divestos.org/fdroid/official/"
    override val displayCategory = DisplayCategory.GOOD_PRIVACY_BROWSER

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestUpdate {
        Log.i(LOG_TAG, "check for latest version")
        val abi = DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)
        val result = CustomRepositoryConsumer.getLatestUpdate(
            "https://divestos.org/fdroid/official",
            packageName,
            abi,
            cacheBehaviour
        )
        Log.i(LOG_TAG, "found latest version ${result.version}")
        return result
    }

    companion object {
        private const val LOG_TAG = "Mulch"
    }
}