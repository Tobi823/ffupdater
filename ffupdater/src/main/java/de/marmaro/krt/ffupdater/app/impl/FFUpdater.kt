package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.OTHER
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.github.GithubConsumer

/**
 * https://api.github.com/repos/Tobi823/ffupdater/releases
 */
@Keep
object FFUpdater : AppBase() {
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
    override val displayCategory = listOf(OTHER)
    override val differentSignatureMessage = R.string.ffupdater__different_signature_message

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val result = GithubConsumer.findLatestRelease(
            repository = GithubConsumer.GithubRepo("Tobi823", "ffupdater"),
            resultsPerApiCall = 5,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(".apk") },
            dontUseApiForLatestRelease = false,
            cacheBehaviour = cacheBehaviour,
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