package de.marmaro.krt.ffupdater.device

import android.os.Build

object DeviceAbiExtractor {
    var supportedAbis: List<ABI> = Build.SUPPORTED_ABIS?.map { ABI.findByCodeName(it) } ?: listOf()

    fun findBestAbi(abisSupportedByApp: List<ABI>, prefer32Bit: Boolean): ABI {
        if (prefer32Bit) {
            return supportedAbis.first { it.is32Bit && it in abisSupportedByApp }
        }
        return supportedAbis.first { it in abisSupportedByApp }
    }

    fun supportsOneOf(abisSupportedByApp: List<ABI>): Boolean {
        return supportedAbis.any { it in abisSupportedByApp }
    }
}