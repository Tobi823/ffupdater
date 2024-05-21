package de.marmaro.krt.ffupdater.device

import android.os.Build

object DeviceAbiExtractor {
    var supportedAbis: List<ABI> = Build.SUPPORTED_ABIS?.map { ABI.findByCodeName(it) } ?: listOf()

    @Throws(IllegalStateException::class)
    fun findBestAbi(abisSupportedByApp: List<ABI>, prefer32Bit: Boolean): ABI {
        val supportedApi = if (prefer32Bit) {
            supportedAbis.firstOrNull { it.is32Bit && it in abisSupportedByApp }
        } else {
            supportedAbis.firstOrNull { it in abisSupportedByApp }
        }
        return checkNotNull(supportedApi) { "Device API is not supported by app." }
    }

    fun supportsOneOf(abisSupportedByApp: List<ABI>): Boolean {
        return supportedAbis.any { it in abisSupportedByApp }
    }

    fun findBestAbiAsStringA(abisSupportedByApp: List<ABI>, prefer32Bit: Boolean): String {
        return when (findBestAbi(abisSupportedByApp, prefer32Bit)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A -> "arm64-v8a"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x86_64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
    }
}