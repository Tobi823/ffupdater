package de.marmaro.krt.ffupdater.device

import android.os.Build

object DeviceAbiExtractor {
    var supportedAbis: List<ABI> = Build.SUPPORTED_ABIS?.map { ABI.findByCodeName(it) } ?: listOf()

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
}