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
    override val downloadSource = R.string.download_source__fdroid
    override val icon = R.mipmap.ic_logo_mull
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "ff81f5be56396594eee70fef2832256e15214122e2ba9cedd26005ffd4bcaaa8"
    override val projectPage = "https://f-droid.org/en/packages/us.spotco.fennec_dos/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.i(LOG_TAG, "check for latest version")
        val result = fdroidConsumer.getLatestUpdate(packageName, context)

        check(result.versionCodesAndDownloadUrls.size == 2)
        val codeAndUrl = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARM64_V8A -> result.versionCodesAndDownloadUrls.maxByOrNull { it.versionCode }!!
            ABI.ARMEABI_V7A -> result.versionCodesAndDownloadUrls.minByOrNull { it.versionCode }!!
            else -> throw IllegalArgumentException("ABI is not supported")
        }

        Log.i(LOG_TAG, "found latest version ${result.versionName}")
        return LatestUpdate(
            downloadUrl = codeAndUrl.downloadUrl,
            version = result.versionName,
            publishDate = null,
            fileSizeBytes = null,
            fileHash = null,
            firstReleaseHasAssets = true
        )
    }

    companion object {
        private const val LOG_TAG = "Mull"
    }
}