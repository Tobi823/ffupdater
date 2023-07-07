package de.marmaro.krt.ffupdater.device

import android.os.Build

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

    fun supportsOneOf(abisSupportedByApp: List<ABI>): Boolean {
        return supportedAbis.any { it in abisSupportedByApp }
    }

    val INSTANCE = DeviceAbiExtractor
}