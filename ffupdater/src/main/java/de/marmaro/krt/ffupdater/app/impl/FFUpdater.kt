package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.github.GithubConsumer
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper

/**
 * https://api.github.com/repos/Tobi823/ffupdater/releases
 */
class FFUpdater(
    private val consumer: GithubConsumer = GithubConsumer.INSTANCE,
) : AppBase() {
    override val app = App.FFUPDATER
    override val codeName = "FFUpdater"
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
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val settings = NetworkSettingsHelper(context)
        val result = consumer.updateCheck(
            repoOwner = "Tobi823",
            repoName = "ffupdater",
            initResultsPerPage = 5,
            isValidRelease = { !it.isPreRelease },
            isSuitableAsset = { it.name.endsWith(".apk") },
            dontUseApiForLatestRelease = false,
            settings = settings
        )
        val version = result.tagName
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = result.url,
            version = version,
            publishDate = result.releaseDate,
            fileSizeBytes = result.fileSizeBytes,
            fileHash = null,
        )
    }

    companion object {
        private const val LOG_TAG = "FFUpdater"
    }
}