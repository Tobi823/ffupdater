package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer

class Mulch(
    private val customRepositoryConsumer: CustomRepositoryConsumer = CustomRepositoryConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "us.spotco.mulch"
    override val title = R.string.mulch__title
    override val description = R.string.mulch__description
    override val installationWarning = R.string.mulch__warning
    override val downloadSource = "https://divestos.org/fdroid/official"
    override val icon = R.mipmap.ic_logo_mulch
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = listOf(ABI.ARM64_V8A)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "260e0a49678c78b70c02d6537add3b6dc0a17171bbde8ce75fd4026a8a3e18d2"
    override val projectPage = "https://divestos.org/fdroid/official/"
    override val displayCategory = DisplayCategory.GOOD_PRIVACY_BROWSER

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.i(LOG_TAG, "check for latest version")
        val abi = deviceAbiExtractor.findBestAbiForDeviceAndApp(supportedAbis)
        val result = customRepositoryConsumer.getLatestUpdate(
            context,
            "https://divestos.org/fdroid/official",
            packageName,
            abi
        )
        Log.i(LOG_TAG, "found latest version ${result.version}")
        return result
    }

    companion object {
        private const val LOG_TAG = "Mulch"
    }
}