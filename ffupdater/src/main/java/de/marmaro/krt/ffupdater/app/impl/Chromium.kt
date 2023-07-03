package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import com.google.gson.annotations.SerializedName
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android/
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android_Arm64/
 */
class Chromium(
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
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
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME
    override val fileNameInZipArchive = "chrome-android/apks/ChromePublic.apk"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val platform = when (deviceAbiExtractor.findBestAbi(supportedAbis, deviceSettings.prefer32BitApks)) {
            ABI.ARM64_V8A -> "Android_Arm64"
            ABI.ARMEABI_V7A -> "Android"
            else -> throw IllegalArgumentException("ABI is not supported")
        }

        val revision = findLatestRevision(fileDownloader, platform)
        val storageObject = findStorageObject(fileDownloader, revision, platform)
        Log.i(LOG_TAG, "found latest version $revision")
        return LatestUpdate(
            downloadUrl = storageObject.downloadUrl,
            version = revision,
            publishDate = storageObject.timestamp,
            exactFileSizeBytesOfDownload = storageObject.fileSizeBytes,
            fileHash = null,
        )
    }

    private suspend fun findLatestRevision(fileDownloader: FileDownloader, platform: String): String {
        return try {
            val slash = "%2F"
            fileDownloader.downloadSmallFileAsString("$BASE_DOWNLOAD_URL/${platform}${slash}LAST_CHANGE?alt=media")
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
    }

    private suspend fun findStorageObject(
        fileDownloader: FileDownloader,
        revision: String,
        platform: String,
    ): StorageObject {
        val url = "$BASE_API_URL?delimiter=/&prefix=$platform/${revision}/chrome-android&$ALL_FIELDS"
        val storageObjects = try {
            fileDownloader.downloadObject(url, StorageObjects::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
        checkNotNull(storageObjects.items)
        check(storageObjects.items.size == 1)

        val storageObject = storageObjects.items[0]
        check(storageObject.name == "$platform/$revision/chrome-android.zip")
        return storageObject
    }

    @SuppressLint("ApplySharedPref")
    override fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(INSTALLED_VERSION_REVISION, available.latestUpdate.version)
            .putString(INSTALLED_VERSION_TIMESTAMP, available.latestUpdate.publishDate)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appIsInstalledCallback(context, available)
    }

    override fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate,
    ): Boolean {
        try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (preferences.getString(INSTALLED_VERSION_REVISION, "-1") != available.version) {
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

    data class StorageObjects(
        @SerializedName("kind")
        val kind: String,
        @SerializedName("items")
        val items: List<StorageObject>,
    )

    data class StorageObject(
        @SerializedName("kind")
        val kind: String,
        @SerializedName("mediaLink")
        val downloadUrl: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("size")
        val fileSizeBytes: Long,
        @SerializedName("updated")
        val timestamp: String,
    )

    companion object {
        private const val LOG_TAG = "Chromium"
        const val INSTALLED_VERSION_REVISION = "chromium__installed_version_revision"
        const val INSTALLED_VERSION_TIMESTAMP = "chromium__installed_version_timestamp"

        const val BASE_API_URL = "https://www.googleapis.com/storage/v1/b/chromium-browser-snapshots/o"
        const val BASE_DOWNLOAD_URL =
            "https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o"
        const val ALL_FIELDS =
            "fields=items(kind,mediaLink,metadata,name,size,updated),kind,prefixes,nextPageToken"
    }
}