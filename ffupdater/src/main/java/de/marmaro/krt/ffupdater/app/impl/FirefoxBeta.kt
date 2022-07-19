package de.marmaro.krt.ffupdater.app.impl

import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.ApiConsumer
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.mozillaci.MozillaCiLogConsumer

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-beta/
 */
class FirefoxBeta(
    private val apiConsumer: ApiConsumer = ApiConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
) : AppBase() {
    override val packageName = "org.mozilla.firefox_beta"
    override val title = R.string.firefox_beta__title
    override val description = R.string.firefox_beta__description
    override val installationWarning = R.string.firefox_beta__warning
    override val downloadSource = R.string.mozilla_ci
    override val icon = R.mipmap.ic_logo_firefox_beta
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM_AND_X_ABIS
    override val projectPage =
        "https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta.latest"

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "a78b62a5165b4494b2fead9e76a280d22d937fee6251aece599446b2ea319b04"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(): LatestUpdate {
        Log.d(LOG_TAG, "check for latest version")
        val abiString = when (deviceAbiExtractor.supportedAbis.first { abi -> abi in supportedAbis }) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A -> "arm64-v8a"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x86_64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        val mozillaCiConsumer = MozillaCiLogConsumer(
            task = "mobile.v2.fenix.beta.latest.$abiString",
            apkArtifact = "public/build/$abiString/target.apk",
            apiConsumer = apiConsumer,
        )
        val result = try {
            mozillaCiConsumer.updateCheck()
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest version of Firefox Beta.", e)
        }
        Log.i(LOG_TAG, "found latest version ${result.version}")
        return LatestUpdate(
            downloadUrl = result.url,
            version = result.version,
            publishDate = result.releaseDate,
            fileSizeBytes = null,
            fileHash = null,
            firstReleaseHasAssets = true,
        )
    }

    companion object {
        private const val LOG_TAG = "FirefoxBeta"
    }
}