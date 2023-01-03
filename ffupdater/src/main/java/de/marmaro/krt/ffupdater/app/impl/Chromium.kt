package de.marmaro.krt.ffupdater.app.impl

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
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import de.marmaro.krt.ffupdater.settings.NetworkSettingsHelper
import java.io.File

/**
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android/
 */
class Chromium(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val app = App.CHROMIUM
    override val codeName = "Chromium"
    override val packageName = "org.chromium.chrome"
    override val title = R.string.chromium__title
    override val description = R.string.chromium__description
    override val installationWarning = R.string.chromium__warning
    override val downloadSource = "https://storage.googleapis.com/chromium-browser-snapshots"
    override val icon = R.mipmap.ic_logo_chromium
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64
    override val signatureHash = "32a2fc74d731105859e5a85df16d95f102d85b22099b8064c5d8915c61dad1e0"
    override val projectPage = "https://www.chromium.org/chromium-projects/"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME
    override val fileNameInZipArchive = "chrome-android/apks/ChromePublic.apk"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val networkSettings = NetworkSettingsHelper(preferences)
        val deviceSettings = DeviceSettingsHelper(preferences)

        val platform = when (deviceAbiExtractor.findBestAbi(supportedAbis, deviceSettings.prefer32BitApks)) {
            ABI.ARM64_V8A -> "Android_Arm64"
            ABI.ARMEABI_V7A -> "Android"
            else -> throw IllegalArgumentException("ABI is not supported")
        }

        val revision = findLatestRevision(networkSettings, platform)
        val storageObject = findStorageObject(networkSettings, revision, platform)
        Log.i(LOG_TAG, "found latest version $revision")
        return LatestUpdate(
            downloadUrl = storageObject.downloadUrl,
            version = revision,
            publishDate = storageObject.timestamp,
            fileSizeBytes = storageObject.fileSizeBytes,
            fileHash = null,
        )
    }

    private suspend fun findLatestRevision(settings: NetworkSettingsHelper, platform: String): String {
        val url = "$BASE_DOWNLOAD_URL/$platform%2FLAST_CHANGE?alt=media"
        val content = try {
            apiConsumer.consume(url, settings)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }

        return content
    }

    private suspend fun findStorageObject(
        settings: NetworkSettingsHelper,
        revision: String,
        platform: String
    ): StorageObject {
        val url = "$BASE_API_URL?delimiter=/&prefix=$platform/${revision}/chrome-android&$ALL_FIELDS"
        val storageObjects = try {
            apiConsumer.consume(url, settings, StorageObjects::class)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
        checkNotNull(storageObjects.items)
        check(storageObjects.items.size == 1)

        val storageObject = storageObjects.items[0]
        check(storageObject.name == "$platform/$revision/chrome-android.zip")
        return storageObject
    }

    override fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(INSTALLED_VERSION_REVISION, available.version)
            .putString(INSTALLED_VERSION_TIMESTAMP, available.publishDate)
            .apply()
        // this must be called last because the update is only recognized after setting the other values
        super.appIsInstalledCallback(context, available)
    }

    override suspend fun isAvailableVersionEqualToArchive(
        context: Context, file: File, available: LatestUpdate,
    ): Boolean {
        // TODO funktioniert nicht bei ZIP-Archiven
        // die APK Dateien brauchen im Dateinamen einen identifier
        // muss aufpassen, dass ich kein Dupletten etc. habe
        // vielleicht eine Prüfsumme vom Identifier?
        return file.length() == available.fileSizeBytes
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