package de.marmaro.krt.ffupdater.device

import android.os.Build
import androidx.annotation.Keep

object DeviceAbiExtractor {
    var supportedAbis: List<ABI> = Build.SUPPORTED_ABIS?.map { ABI.findByCodeName(it) } ?: listOf()

    fun findBestAbi(abisSupportedByApp: List<ABI>, prefer32Bit: Boolean): ABI {
        return supportedAbis
            .firstOrNull {
                if (prefer32Bit) {
                    it.is32Bit && it in abisSupportedByApp
                } else {
                    it in abisSupportedByApp
                }
            }
            ?: throw NoSuchElementException(
                "Your device is not supported. The app needs $abisSupportedByApp but your device has only $supportedAbis."
            )
    }

    @Keep
    data class StringsForAbi(
        val armeabi_v7a: String? = null,
        val arm64_v8a: String? = null,
        val x86: String? = null,
        val x86_64: String? = null,

        )

    fun findStringForBestAbi(stringsForAbi: StringsForAbi, prefer32Bit: Boolean): String {
        val supportedAbis = mutableListOf<ABI>()
        if (stringsForAbi.armeabi_v7a != null) {
            supportedAbis.add(ABI.ARMEABI_V7A)
        }
        if (stringsForAbi.arm64_v8a != null) {
            supportedAbis.add(ABI.ARM64_V8A)
        }
        if (stringsForAbi.x86 != null) {
            supportedAbis.add(ABI.X86)
        }
        if (stringsForAbi.x86_64 != null) {
            supportedAbis.add(ABI.X86_64)
        }
        return when (findBestAbi(supportedAbis, prefer32Bit)) {
            ABI.ARM64_V8A -> stringsForAbi.arm64_v8a!!
            ABI.ARMEABI_V7A -> stringsForAbi.armeabi_v7a!!
            ABI.ARMEABI -> throw IllegalArgumentException("ABI.ARMEABI is not supported")
            ABI.X86 -> stringsForAbi.x86!!
            ABI.X86_64 -> stringsForAbi.x86_64!!
            ABI.MIPS -> throw IllegalArgumentException("ABI.MIPS is not supported")
            ABI.MIPS64 -> throw IllegalArgumentException("ABI.MIPS64 is not supported")
        }
    }

    fun supportsOneOf(abisSupportedByApp: List<ABI>): Boolean {
        return supportedAbis.any { it in abisSupportedByApp }
    }


    val INSTANCE = DeviceAbiExtractor

}