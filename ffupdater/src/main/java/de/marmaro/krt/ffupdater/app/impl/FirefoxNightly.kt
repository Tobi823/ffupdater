package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.graphics.Color
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
class FirefoxNightly(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.mozilla.fenix"
    override val displayTitle = R.string.firefox_nightly__title
    override val displayDescription = R.string.firefox_nightly__description
    override val displayWarning = R.string.firefox_nightly__warning
    override val displayDownloadSource = R.string.mozilla_ci
    override val displayIcon = R.mipmap.ic_logo_firefox_nightly
    override val displayIconBackground = Color.parseColor("#FFFFFF")
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)
    @Suppress("SpellCheckingInspection")
    override val signatureHash = "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211"

    override fun getDisplayInstalledVersion(context: Context): String {
        val rawVersion = getInstalledVersion(context) ?:
            return context.getString(R.string.installed_version, "")

        val importantVersionPart = Regex("""^(Nightly \d{6} \d{2}):\d{2}$""")
                .find(rawVersion)!!
                .groups[1]!!.value
        val version = "${importantVersionPart}:xx"
        return context.getString(R.string.installed_version, version)
    }

    override suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment): AvailableVersionResult {
        val abiString = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> "arm64-v8a"
                ABI.ARMEABI_V7A -> "armeabi-v7a"
                ABI.X86 -> "x86"
                ABI.X86_64 -> "x86_64"
                ABI.ARMEABI, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
        val mozillaCiConsumer = MozillaCiConsumer(
                apiConsumer = apiConsumer,
                task = "mobile.v2.fenix.nightly.latest.$abiString",
                apkArtifact = "public/build/$abiString/target.apk",
                keyForVersion = "tag_name",
                keyForReleaseDate = "now")
        val result = mozillaCiConsumer.updateCheck()
        val formatter = DateTimeFormatter.ofPattern("yyMMdd HH")
        val timestamp = formatter.format(result.releaseDate)
        val version = "Nightly ${timestamp}:xx"
        return AvailableVersionResult(
                downloadUrl = result.url,
                version = version,
                publishDate = result.releaseDate,
                fileSizeBytes = null)
    }

    override fun areVersionsDifferent(
            installedVersion: String?,
            available: AvailableVersionResult): Boolean {
        if (installedVersion == null) {
            return true
        }
        val regex = Regex("""Nightly (\d{6} \d{2}):[\dx]{2}$""")
        val installedString = regex.find(installedVersion)!!.groups[1]!!.value
        val availableString = regex.find(available.version)!!.groups[1]!!.value
        val pattern = DateTimeFormatter.ofPattern("yyMMdd HH")
        val installedDateTime = LocalDateTime.parse(installedString, pattern)
        val availableDateTime = LocalDateTime.parse(availableString, pattern)
        return availableDateTime.isAfter(installedDateTime)
    }
}