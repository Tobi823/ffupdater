package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

@Keep
object FennecFdroid : AppBase() {
    override val app = App.FENNEC_FDROID
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
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestUpdate {
        val index = findIndex()
        val result = FdroidConsumer.getLatestUpdate(packageName, index, cacheBehaviour)
        return LatestUpdate(
            downloadUrl = result.downloadUrl,
            version = result.versionName,
            publishDate = result.createdAt,
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }

    private fun findIndex(): Int {
        val index = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> 1
            ABI.ARM64_V8A -> 2
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return index
    }
}