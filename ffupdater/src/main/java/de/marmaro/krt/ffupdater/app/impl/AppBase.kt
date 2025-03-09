package de.marmaro.krt.ffupdater.app.impl

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.impl.base.ApkDownloader
import de.marmaro.krt.ffupdater.app.impl.base.AppAttributes
import de.marmaro.krt.ffupdater.app.impl.base.InstalledAppStatusFetcher
import de.marmaro.krt.ffupdater.app.impl.base.InstalledVersionFetcher
import de.marmaro.krt.ffupdater.app.impl.base.LatestVersionFetcher
import de.marmaro.krt.ffupdater.app.impl.base.VersionDisplay
import de.marmaro.krt.ffupdater.device.ABI.ARM64_V8A
import de.marmaro.krt.ffupdater.device.ABI.ARMEABI
import de.marmaro.krt.ffupdater.device.ABI.ARMEABI_V7A
import de.marmaro.krt.ffupdater.device.ABI.MIPS
import de.marmaro.krt.ffupdater.device.ABI.MIPS64
import de.marmaro.krt.ffupdater.device.ABI.X86
import de.marmaro.krt.ffupdater.device.ABI.X86_64


@Keep
abstract class AppBase : AppAttributes, ApkDownloader, LatestVersionFetcher, InstalledVersionFetcher, VersionDisplay,
    InstalledAppStatusFetcher {
    override val installationWarning: Int? = null
    override val installableByUser = true
    override val eolReason: Int? = null
    override val fileNameInZipArchive: String? = null
    override val differentSignatureMessage: Int = R.string.main_activity__app_was_signed_by_different_certificate

    companion object {
        val ALL_ABIS = listOf(ARM64_V8A, ARMEABI_V7A, ARMEABI, X86_64, X86, MIPS, MIPS64)
        val ARM32_ARM64_X86_X64 = listOf(ARM64_V8A, ARMEABI_V7A, X86_64, X86)
        val ARM32_ARM64 = listOf(ARM64_V8A, ARMEABI_V7A)
    }
}