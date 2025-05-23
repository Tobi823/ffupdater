package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import com.google.gson.JsonObject
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.BETTER_THAN_GOOGLE_CHROME
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.app.entity.Version
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android/
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android_Arm64/
 */
@Keep
object Chromium : AppBase() {
    override val app = App.CHROMIUM
    override val packageName = "org.chromium.chrome"
    override val title = R.string.chromium__title
    override val description = R.string.chromium__description
    override val installationWarning = R.string.chromium__warning
    override val downloadSource = "https://storage.googleapis.com/chromium-browser-snapshots"
    override val icon = R.drawable.ic_logo_chromium
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64
    override val signatureHash = "32a2fc74d731105859e5a85df16d95f102d85b22099b8064c5d8915c61dad1e0"
    override val projectPage = "https://www.chromium.org/chromium-projects/"
    override val displayCategory = listOf(BETTER_THAN_GOOGLE_CHROME)
    override val fileNameInZipArchive = "chrome-android/apks/ChromePublic.apk"
    override val hostnameForInternetCheck = "https://storage.googleapis.com"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context): LatestVersion {
        val platform = findPlatform()
        val revision = findLatestRevision(platform)
        val storageObject = findStorageObject(revision, platform)
        return LatestVersion(
            downloadUrl = storageObject.downloadUrl,
            version = Version(revision),
            publishDate = storageObject.timestamp,
            exactFileSizeBytesOfDownload = storageObject.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findPlatform(): String {
        val platform = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARM64_V8A -> "Android_Arm64"
            ABI.ARMEABI_V7A -> "Android"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return platform
    }

    private suspend fun findLatestRevision(platform: String): String {
        val slash = "%2F"
        val url = "$BASE_DOWNLOAD_URL/${platform}${slash}LAST_CHANGE?alt=media"
        return FileDownloader.downloadString(url)
    }

    private suspend fun findStorageObject(
        revision: String,
        platform: String,
    ): StorageObject {
        val url = "$BASE_API_URL?delimiter=/&prefix=$platform/${revision}/chrome-android&$ALL_FIELDS"
        val storageObjects = FileDownloader.downloadAsJsonObject(url)
        return parseJson(storageObjects, platform, revision)
    }

    @Throws(IllegalStateException::class)
    private suspend fun parseJson(
        storageObjects: JsonObject,
        platform: String,
        revision: String,
    ): StorageObject {
        val storageObjectKotlin = try {
            withContext(Dispatchers.Default) {
                val items = storageObjects["items"].asJsonArray
                val storageObject = items[0].asJsonObject
                StorageObject(
                    kind = storageObject["kind"].asString,
                    downloadUrl = storageObject["mediaLink"].asString,
                    name = storageObject["name"].asString,
                    fileSizeBytes = storageObject["size"].asLong,
                    timestamp = storageObject["updated"].asString,
                )
            }
        } catch (e: CancellationException) {
            throw e // CancellationException is normal and should not treat as error
        } catch (e: Exception) {
            when (e) {
                is NullPointerException,
                is NumberFormatException,
                is IllegalStateException,
                is UnsupportedOperationException,
                is IndexOutOfBoundsException,
                -> throw NetworkException("Returned JSON is incorrect. Try delete the cache of FFUpdater.", e)
            }
            throw e
        }

        check(storageObjectKotlin.kind == "storage#object")
        check(storageObjectKotlin.name == "$platform/$revision/chrome-android.zip")
        return storageObjectKotlin
    }

    @SuppressLint("ApplySharedPref")
    override suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(INSTALLED_VERSION_REVISION, available.latestVersion.version.versionText)
            .putString(INSTALLED_VERSION_TIMESTAMP, available.latestVersion.publishDate)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appWasInstalledCallback(context, available)
    }

    override suspend fun isInstalledAppOutdated(
        context: Context,
        available: LatestVersion,
    ): Boolean {
        try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (preferences.getString(INSTALLED_VERSION_REVISION, "-1") != available.version.versionText) {
                return true
            }
            if (preferences.getString(INSTALLED_VERSION_TIMESTAMP, "") != available.publishDate) {
                return true
            }
            return false
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }

    @Keep
    private data class StorageObject(
        val kind: String,
        val downloadUrl: String,
        val name: String,
        val fileSizeBytes: Long,
        val timestamp: String,
    )

    private const val INSTALLED_VERSION_REVISION = "chromium__installed_version_revision"
    private const val INSTALLED_VERSION_TIMESTAMP = "chromium__installed_version_timestamp"

    private const val BASE_API_URL = "https://www.googleapis.com/storage/v1/b/chromium-browser-snapshots/o"
    private const val BASE_DOWNLOAD_URL =
        "https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o"
    private const val ALL_FIELDS =
        "fields=items(kind,mediaLink,metadata,name,size,updated),kind,prefixes,nextPageToken"
}