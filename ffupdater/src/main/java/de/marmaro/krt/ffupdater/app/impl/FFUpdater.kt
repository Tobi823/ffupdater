package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://api.github.com/repos/Tobi823/ffupdater/releases
 */
@Keep
class FFUpdater : AppBase() {
    override val app = App.FFUPDATER
    override val packageName = "de.marmaro.krt.ffupdater"
    override val title = R.string.app_name
    override val description = R.string.app_description
    override val downloadSource = "GitHub"
    override val icon = R.mipmap.ic_launcher
    override val minApiLevel = Build.VERSION_CODES.N
    override val supportedAbis = ALL_ABIS

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "f4e642bb85cbbcfd7302b2cbcbd346993a41067c27d995df492c9d0d38747e62"
    override val installableByUser = false
    override val projectPage = "https://github.com/Tobi823/ffupdater"
    override val displayCategory = DisplayCategory.OTHER

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("Tobi823", "ffupdater"),
            resultsPerApiCall = 5,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(".apk") },
            dontUseApiForLatestRelease = false,
            cacheBehaviour = cacheBehaviour,
        )
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
        private const val LOG_TAG = "FFUpdater"
    }
}