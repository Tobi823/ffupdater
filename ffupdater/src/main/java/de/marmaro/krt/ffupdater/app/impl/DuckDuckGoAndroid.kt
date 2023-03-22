package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://github.com/duckduckgo/Android/releases
 * https://api.github.com/repos/duckduckgo/Android/releases
 * https://www.apkmirror.com/apk/duckduckgo/duckduckgo-privacy-browser/
 */
class DuckDuckGoAndroid(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
) : AppBase() {
    override val app = App.DUCKDUCKGO_ANDROID
    override val packageName = "com.duckduckgo.mobile.android"
    override val title = R.string.duckduckgo_android__title
    override val description = R.string.duckduckgo_android__description
    override val installationWarning = R.string.duckduckgo_android__warning
    override val downloadSource = "GitHub"
    override val icon = R.drawable.ic_logo_duckduckgo_android
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ALL_ABIS

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "bb7bb31c573c46a1da7fc5c528a6acf432108456feec50810c7f33694eb3d2d4"
    override val projectPage = "https://github.com/duckduckgo/Android"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val networkSettings = NetworkSettingsHelper(context)

        val result = consumer.updateCheck(
            repoOwner = "duckduckgo",
            repoName = "Android",
            initResultsPerPage = 3,
            isValidRelease = { true },
            isSuitableAsset = { it.nameStartsAndEndsWith("duckduckgo-", "-play-release.apk") },
            dontUseApiForLatestRelease = false,
            fileDownloader = fileDownloader,
        )
        // tag name can be "5.45.4"
        val version = result.tagName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }

    companion object {
        private const val LOG_TAG = "DuckDuckGo Android"
    }
}