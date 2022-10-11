package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer

/**
 * https://f-droid.org/en/packages/us.spotco.fennec_dos/
 */
class MullFromRepo(
    private val customRepositoryConsumer: CustomRepositoryConsumer = CustomRepositoryConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "us.spotco.fennec_dos"
    override val title = R.string.mull__title
    override val description = R.string.mull__description
    override val downloadSource = "https://divestos.org/fdroid/official"
    override val icon = R.mipmap.ic_logo_mull
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "e4be8d6abfa4d9d4feef03cdda7ff62a73fd64b75566f6dd4e5e577550be8467"
    override val projectPage = "https://divestos.org/fdroid/official/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

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
        private const val LOG_TAG = "MullFromRepo"
    }
}