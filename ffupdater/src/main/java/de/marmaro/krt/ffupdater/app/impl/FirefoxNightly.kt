package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.UpdateCheckSubResult
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiConsumer
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceEnvironment

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 */
class FirefoxNightly(private val apiConsumer: ApiConsumer) : BaseAppDetail() {
    override val packageName = "org.mozilla.fenix"
    override val displayTitle = R.string.firefox_nightly_title
    override val displayDescription = R.string.firefox_nightly_description
    override val displayWarning = R.string.firefox_nightly_warning
    override val displayDownloadSource = R.string.mozilla_ci
    override val signatureHash = "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211"
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A, ABI.X86_64, ABI.X86)

    override fun getInstalledVersion(context: Context): String? {
        val rawVersion = super.getInstalledVersion(context) ?: return null
        return Regex("""^(Nightly \d{6}) \d{2}:\d{2}$""").find(rawVersion)!!
                .groups[1]!!.value
    }

    override fun updateCheckBlocking(context: Context,
                                     deviceEnvironment: DeviceEnvironment): UpdateCheckSubResult {
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
        val year = (result.releaseDate.year-2000).toString()
        val monthValue = result.releaseDate.monthValue
        val month = if (monthValue < 10) "0$monthValue" else monthValue.toString()
        val dayValue = result.releaseDate.dayOfMonth
        val day = if (dayValue < 10) "0$dayValue" else dayValue.toString()
        val version = "Nightly $year$month$day"
        return UpdateCheckSubResult(
                downloadUrl = result.url,
                version = version,
                displayVersion = context.getString(R.string.available_version, version),
                publishDate = result.releaseDate,
                fileSizeBytes = null)
    }
}