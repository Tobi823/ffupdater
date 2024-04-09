package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory.FROM_MOZILLA
import de.marmaro.krt.ffupdater.app.entity.InstalledAppStatus
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.mozillaci.MozillaCiJsonConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/gecko.v2.mozilla-central.latest.mobile.fenix-nightly
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
@Keep
object FirefoxNightly : AppBase() {
    override val app = App.FIREFOX_NIGHTLY
    override val packageName = "org.mozilla.fenix"
    override val title = R.string.firefox_nightly__title
    override val description = R.string.firefox_nightly__description
    override val installationWarning = R.string.generic_app_warning__beta_version
    override val downloadSource = "Mozilla CI"
    override val icon = R.drawable.ic_logo_firefox_nightly
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP

    private const val HOSTNAME = "https://firefox-ci-tc.services.mozilla.com"
    private const val TASK_PARENT_NAMESPACE = "gecko.v2.mozilla-central.latest.mobile"
    private const val TASK_NAMESPACE = "$TASK_PARENT_NAMESPACE.fenix-nightly"

    override val signatureHash = "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211"
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val projectPage = "$HOSTNAME/tasks/index/$TASK_PARENT_NAMESPACE/fenix-nightly"
    override val displayCategory = listOf(FROM_MOZILLA)

    private val versionDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val abiString = findAbiString()
        val taskId = MozillaCiJsonConsumer.findTaskId(TASK_NAMESPACE, cacheBehaviour)
        val metaInformation = MozillaCiJsonConsumer.findChainOfTrustJson(taskId, abiString, cacheBehaviour)
        return LatestVersion(
            downloadUrl = getDownloadUrl(abiString),
            version = formatReleaseDate(metaInformation.releaseDate),
            publishDate = metaInformation.releaseDate,
            exactFileSizeBytesOfDownload = null,
            fileHash = metaInformation.fileHash,
        )
    }

    private fun findAbiString(): String {
        val abiString = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A -> "arm64-v8a"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x86_64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return abiString
    }

    private fun getDownloadUrl(abiString: String): String {
        return "$HOSTNAME/api/index/v1/task/$TASK_NAMESPACE/artifacts/public%2Fbuild%2Ftarget.$abiString.apk"
    }

    private fun formatReleaseDate(releaseDate: String): String {
        val date = ZonedDateTime.parse(releaseDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        return versionDateFormat.format(date)
    }

    override suspend fun isInstalledAppOutdated(
        context: Context,
        available: LatestVersion,
    ): Boolean {
        return try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val installedSha256Hash = preferences.getString(INSTALLED_SHA256_HASH, "unknown")
            val installedVersionCode = preferences.getLong(INSTALLED_VERSION_CODE, -1)
            val sameHex = available.fileHash?.hexValue == installedSha256Hash
            val sameVersionCode = getVersionCode(context) == installedVersionCode
            !(sameHex && sameVersionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @SuppressLint("ApplySharedPref")
    override suspend fun appWasInstalledCallback(context: Context, available: InstalledAppStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putLong(INSTALLED_VERSION_CODE, getVersionCode(context))
            .putString(INSTALLED_SHA256_HASH, available.latestVersion.fileHash?.hexValue)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appWasInstalledCallback(context, available)
    }

    /**
     * @throws PackageManager.NameNotFoundException
     */
    @Suppress("DEPRECATION")
    private fun getVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        if (DeviceSdkTester.supportsAndroid9P28()) {
            return packageInfo.longVersionCode
        }
        return packageInfo.versionCode.toLong()
    }

    private const val INSTALLED_VERSION_CODE = "firefox_nightly_installed_version_code"
    private const val INSTALLED_SHA256_HASH = "firefox_nightly_installed_sha256_hash"
}