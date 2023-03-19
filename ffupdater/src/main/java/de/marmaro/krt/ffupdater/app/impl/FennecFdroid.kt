package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

class FennecFdroid(
    private val fdroidConsumer: FdroidConsumer = FdroidConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.FENNEC_FDROID
    override val codeName = "FennecFdroid"
    override val packageName = "org.mozilla.fennec_fdroid"
    override val title = R.string.fennecfdroid__title
    override val description = R.string.fennecfdroid__description
    override val installationWarning = R.string.fennecfdroid__warning
    override val downloadSource = "F-Droid"
    override val icon = R.drawable.ic_logo_fennec_fdroid
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "06665358efd8ba05be236a47a12cb0958d7d75dd939d77c2b31f5398537ebdc5"
    override val projectPage = "https://f-droid.org/packages/org.mozilla.fennec_fdroid/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate? {
        Log.i(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val index = when (deviceAbiExtractor.findBestAbi(supportedAbis, deviceSettings.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> 1
            ABI.ARM64_V8A -> 2
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val result = fdroidConsumer.getLatestUpdate(packageName, fileDownloader, index)

        val version = result.versionName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.downloadUrl,
            version = version,
            publishDate = result.createdAt,
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    companion object {
        private const val LOG_TAG = "Fennec F-Droid"
    }
}