package de.marmaro.krt.ffupdater.device

import android.os.Build

class DeviceAbiExtractor {
    private val supportedAbiStrings: Array<String> = Build.SUPPORTED_ABIS ?: emptyArray()
    private val supportedAbis = supportedAbiStrings.map { ABI.findByCodeName(it) }

    fun findBestAbiForDeviceAndApp(abisSupportedByApp: List<ABI>): ABI {
        return supportedAbis.firstOrNull { it in abisSupportedByApp }
            ?: throw Exception("The app does not support the device")
    }

    companion object {
        val INSTANCE = DeviceAbiExtractor()
    }
}