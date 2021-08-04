package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import de.marmaro.krt.ffupdater.app.BaseAppDetail
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.mozillaci.MozillaCiLogConsumer
import de.marmaro.krt.ffupdater.device.ABI

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
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = listOf(ABI.ARM64_V8A, ABI.ARMEABI_V7A)

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "6203a473be36d64ee37f87fa500edbc79eab930610ab9b9fa4ca7d5c1f1b4ffc"

    override fun getInstalledVersion(context: Context): String? {
        val originalVersion = super.getInstalledVersion(context)
        return if (originalVersion?.contains("-") == true) {
            originalVersion.split("-")[0]
        } else {
            originalVersion
        }
    }

    override suspend fun updateCheckWithoutCaching(): AvailableVersionResult {
        val abiString = getStringForCurrentAbi("armeabi-v7a", "arm64-v8a", null, null)
        val mozillaCiConsumer = MozillaCiLogConsumer(
            apiConsumer = apiConsumer,
            task = "project.mobile.focus.release.latest",
            apkArtifact = "public/app-focus-$abiString-release-unsigned.apk",
        )
        val result = mozillaCiConsumer.updateCheck()
        return AvailableVersionResult(
            downloadUrl = result.url,
            version = result.version,
            publishDate = result.releaseDate,
            fileSizeBytes = null,
            fileHash = null
        )
    }
}