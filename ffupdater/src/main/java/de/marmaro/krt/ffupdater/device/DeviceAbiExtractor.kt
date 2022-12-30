package de.marmaro.krt.ffupdater.device

import android.os.Build

class DeviceAbiExtractor {
    val supportedAbis = Build.SUPPORTED_ABIS?.map { ABI.findByCodeName(it) } ?: listOf()
    val supported32BitAbis = supportedAbis.filter { it.is32Bit }

    /**
     * findBestAbiForDeviceAndApp
     */
    fun findBestAbi(abisSupportedByApp: List<ABI>, prefer32Bit: Boolean): ABI {
        if (prefer32Bit) {
            supported32BitAbis
                .firstOrNull { it in abisSupportedByApp }
                ?.let { return it }
            // if there is no 32bit abi, then fallback to the default search method
        }

        return supportedAbis
            .firstOrNull { it in abisSupportedByApp }
            ?: throw NoSuchElementException(
                "Your device is not supported. The app needs $abisSupportedByApp but your device has only $supportedAbis."
            )
    }

    fun supportsOneOf(abisSupportedByApp: List<ABI>): Boolean {
        return supportedAbis.any { it in abisSupportedByApp }
    }

    companion object {
        val INSTANCE = DeviceAbiExtractor()
    }
}