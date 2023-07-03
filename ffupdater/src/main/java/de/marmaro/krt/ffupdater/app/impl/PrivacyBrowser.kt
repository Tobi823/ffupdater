package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer

/**
 * https://f-droid.org/en/packages/com.stoutner.privacybrowser.standard/
 */
class PrivacyBrowser(
    private val fdroidConsumer: FdroidConsumer = FdroidConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.PRIVACY_BROWSER
    override val packageName = "com.stoutner.privacybrowser.standard"
    override val title = R.string.privacy_browser__title
    override val description = R.string.privacy_browser__description
    override val installationWarning = R.string.privacy_browser__warning
    override val downloadSource = "F-Droid"
    override val icon = R.drawable.ic_logo_privacybrowser
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ALL_ABIS

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "f97614dc96964bea2e4fa66b24608a510a87b3b1e01ba68e0753c099133a8768"
    override val projectPage = "https://f-droid.org/en/packages/com.stoutner.privacybrowser.standard/"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate {
        Log.i(LOG_TAG, "check for latest version")
        val result = fdroidConsumer.getLatestUpdate(packageName, fileDownloader, 1)
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