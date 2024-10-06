package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.GOOD_PRIVACY_BROWSER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.FdroidConsumer

/**
 * https://f-droid.org/en/packages/com.stoutner.privacybrowser.standard/
 */
@Keep
object PrivacyBrowser : AppBase() {
    override val app = App.PRIVACY_BROWSER
    override val packageName = "com.stoutner.privacybrowser.standard"
    override val title = R.string.privacy_browser__title
    override val description = R.string.privacy_browser__description
    override val installationWarning = R.string.privacy_browser__warning
    override val downloadSource = "F-Droid"
    override val icon = R.drawable.ic_logo_privacybrowser
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ALL_ABIS

    override val signatureHash = "f97614dc96964bea2e4fa66b24608a510a87b3b1e01ba68e0753c099133a8768"
    override val projectPage = "https://f-droid.org/en/packages/com.stoutner.privacybrowser.standard/"
    override val displayCategory = listOf(GOOD_PRIVACY_BROWSER)
    override val hostnameForInternetCheck = "https://f-droid.org"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val result = FdroidConsumer.getLatestUpdate(packageName, { true }, context)
        return LatestVersion(
            downloadUrl = result.downloadUrl,
            version = Version(result.versionName),
            publishDate = result.createdAt,
            exactFileSizeBytesOfDownload = null,
            fileHash = null,
        )
    }
}