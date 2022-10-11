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
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer

/**
 * https://f-droid.org/en/packages/us.spotco.fennec_dos/
 */
class Mull(
    private val fdroidConsumer: FdroidConsumer = FdroidConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "us.spotco.fennec_dos"
    override val title = R.string.mull__title
    override val description = R.string.mull__description
    override val installationWarning = R.string.mull__warning
    override val downloadSource = "F-Droid"
    override val icon = R.mipmap.ic_logo_mull
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "ff81f5be56396594eee70fef2832256e15214122e2ba9cedd26005ffd4bcaaa8"
    override val installableWithDefaultPermission = false // MullFromRepo should be used
    override val projectPage = "https://f-droid.org/en/packages/us.spotco.fennec_dos/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.i(LOG_TAG, "check for latest version")
        val index = when (deviceAbiExtractor.findBestAbiForDeviceAndApp(supportedAbis)) {
            ABI.ARMEABI_V7A -> 1
            ABI.ARM64_V8A -> 2
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = fdroidConsumer.getLatestUpdate(packageName, context, index)

        Log.i(LOG_TAG, "found latest version ${result.versionName}")
        return LatestUpdate(
            downloadUrl = result.downloadUrl,
            version = result.versionName,
            publishDate = result.createdAt
        )
    }

    companion object {
        private const val LOG_TAG = "Mull"
    }
}