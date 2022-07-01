package de.marmaro.krt.ffupdater.device

import android.os.Build

class DeviceAbiExtractor {
    val supportedAbis = findSupportedAbis()

    private fun findSupportedAbis(): List<ABI> {
        val supportedAbis = Build.SUPPORTED_ABIS ?: return listOf()
        return supportedAbis.mapNotNull {
            when (it) {
                "arm64-v8a" -> ABI.ARM64_V8A
                "armeabi-v7a" -> ABI.ARMEABI_V7A
                "armeabi" -> ABI.ARMEABI
                "x86_64" -> ABI.X86_64
                "x86" -> ABI.X86
                "mips" -> ABI.MIPS
                "mips64" -> ABI.MIPS64
                else -> throw IllegalArgumentException("Unknown ABI '$it'")
            }
        }
    }

    companion object {
        val INSTANCE = DeviceAbiExtractor()
    }
}