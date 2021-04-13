package de.marmaro.krt.ffupdater.app.impl

import android.graphics.Color
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/project.mobile.focus.release/latest
 * https://www.apkmirror.com/apk/mozilla/firefox-focus-private-browser/
 */
class FirefoxFocus(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.mozilla.focus"
    override val displayTitle = R.string.firefox_focus__title
    override val displayDescription = R.string.firefox_focus__description
    override val displayWarning: Int? = null
    override val displayDownloadSource = R.string.mozilla_ci
    override val displayIcon = R.mipmap.ic_logo_firefox_focus_klar
    override val displayIconBackground = Color.parseColor("#A4007F")
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A)
    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"

    override suspend fun updateCheckWithoutCaching(deviceEnvironment: DeviceEnvironment): AvailableVersionResult {
        val abiString = deviceEnvironment.abis.mapNotNull {
            when (it) {
                ABI.ARM64_V8A -> "aarch64"
                ABI.ARMEABI_V7A -> "arm"
                ABI.ARMEABI, ABI.X86, ABI.X86_64, ABI.MIPS, ABI.MIPS64 -> null
            }
        }.first()
        val mozillaCiConsumer = MozillaCiConsumer(
                apiConsumer = apiConsumer,
                task = "project.mobile.focus.release.latest",
                apkArtifact = "public/app-focus-$abiString-release-unsigned.apk",
                keyForVersion = "tag_name",
                keyForReleaseDate = "published_at")
        val result = mozillaCiConsumer.updateCheck()
        val version = Regex("""^v(.*)$""").find(result.version)!!.groups[1]!!.value
        return AvailableVersionResult(
                downloadUrl = result.url,
                version = version,
                publishDate = result.releaseDate,
                fileSizeBytes = null)
    }
}