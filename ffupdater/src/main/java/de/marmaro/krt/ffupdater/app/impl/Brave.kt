package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.BaseApp
import de.marmaro.krt.ffupdater.app.UpdateCheckResult
import de.marmaro.krt.ffupdater.app.impl.fetch.ApiConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.GithubConsumer
import de.marmaro.krt.ffupdater.app.impl.fetch.github.dao.Asset
import de.marmaro.krt.ffupdater.app.impl.fetch.github.dao.Release
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.utils.ParamRuntimeException
import java.util.*
import java.util.Map
import kotlin.collections.List

/**
 * https://api.github.com/repos/brave/brave-browser/releases
 */
class Brave : BaseApp() {
    override val packageName = "com.brave.browser"

    override fun getDisplayTitle(context: Context): String {
        var test = R.string.brave_description
        var test2 = test
        return context.getString(R.string.brave_title)
    }

    override fun getDisplayDescription(context: Context): String {
        return context.getString(R.string.brave_description)
    }

    override fun getDisplayWarning(context: Context): Optional<String>? {
        return Optional.of(context.getString(R.string.brave_warning))
    }

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "9c2db70513515fdbfbbc585b3edf3d7123d4dc67c94ffd306361c1d79bbf18ac"

    override fun getDisplayDownloadSource(context: Context): String {
        return context.getString(R.string.github)
    }

    override fun getDisplayInstalledVersion(context: Context): String? {
        return getInstalledVersion(context)
    }

    override fun getInstalledVersion(context: Context): String? {
        return getInstalledVersionFromPackageManager(context)
    }

    override val minApiLevel: Int = Build.VERSION_CODES.N
    override val supportedAbi: List<ABI> = listOf(ABI.AARCH64, ABI.ARM, ABI.X86_64, ABI.X86)

    override fun updateCheck(context: Context, abi: ABI): UpdateCheckResult {
        @Suppress("SpellCheckingInspection")
        val fileName = when (abi) {
            ABI.AARCH64 -> "BraveMonoarm64.apk"
            ABI.ARM -> "BraveMonoarm.apk"
            ABI.X86 -> "BraveMonox86.apk"
            ABI.X86_64 -> "BraveMonox64.apk"
        }

        val result = GithubConsumer.Builder()
                .setApiConsumer(ApiConsumer())
                .setRepoOwner("brave")
                .setRepoName("brave-browser")
                .setResultsPerPage(20)
                .setValidReleaseTester { release: Release ->
                    !release.isPreRelease &&
                            release.assets.stream().map { obj: Asset -> obj.name }.anyMatch { name: String -> name.endsWith(".apk") }
                }
                .setCorrectDownloadUrlTester { asset: Asset -> asset.name == getFileNameForAbi(abi) }
                .build()
                .updateCheck()
        val version = result.tagName.replace("v", "")
        val update = getInstalledVersion(context)!!.map { x: String -> x != version }.orElse(true)
        return Builder()
                .setUpdateAvailable(update)
                .setDownloadUrl(result.url)
                .setVersion(version)
                .setMetadata(Map.of(UpdateCheckResult.FILE_SIZE_BYTES, result.fileSizeBytes))
                .build()
    }

    private fun getFileNameForAbi(abi: ABI): String {
        return when (abi) {
            ABI.AARCH64 -> "BraveMonoarm64.apk"
            ABI.ARM -> "BraveMonoarm.apk"
            ABI.X86 -> "BraveMonox86.apk"
            ABI.X86_64 -> "BraveMonox64.apk"
        }
    }

    override fun installationCallback(context: Context, installedVersion: String) {}
}