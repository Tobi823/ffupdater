package de.marmaro.krt.ffupdater.device

import android.os.Build

class DeviceAbiExtractor {
    private val supportedAbiStrings: Array<String> = Build.SUPPORTED_ABIS ?: emptyArray()
    private val supportedAbis = supportedAbiStrings.map { ABI.findByCodeName(it) }

    fun findBestAbiForDeviceAndApp(abisSupportedByApp: List<ABI>): ABI {
        return try {
            supportedAbis.first { it in abisSupportedByApp }
        } catch (e: NoSuchElementException) {
            throw NoSuchElementException(
                "The device is not supported by the app. It wants $abisSupportedByApp but the device has " +
                        "only $supportedAbis. ${e.message}"
            )
        }
    }

    companion object {
        val INSTANCE = DeviceAbiExtractor()
    }
}