package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BETTER_THAN_GOOGLE_CHROME
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://github.com/duckduckgo/Android/releases
 * https://api.github.com/repos/duckduckgo/Android/releases
 * https://www.apkmirror.com/apk/duckduckgo/duckduckgo-privacy-browser/
 */
@Keep
object DuckDuckGoAndroid : AppBase() {
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
    override val displayCategory = listOf(BETTER_THAN_GOOGLE_CHROME)

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("duckduckgo", "Android", 0),
            isValidRelease = { true },
            isSuitableAsset = { it.nameStartsAndEndsWith("duckduckgo-", "-play-release.apk") },
            cacheBehaviour = cacheBehaviour,
            requireReleaseDescription = false,
        )
        return LatestVersion(
            downloadUrl = result.url,
            version = result.tagName,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = result.fileSizeBytes,
            fileHash = null,
        )
    }
}