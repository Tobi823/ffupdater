package de.marmaro.krt.ffupdater.device

import android.os.Build
import android.os.Build.VERSION.SDK_INT

/**
 * This class returns all supported ABIs and the API level.
 * This indirection with abis and supportAndroidXXX() is necessary, because Mockk can't mock/change the
 * Android classes.
 */
object DeviceEnvironment {
    val abis = findSupportedAbis()
    val sdkInt = SDK_INT

    /**
     * API level 23
     */
    fun supportsAndroidMarshmallow(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.M
    }

    /**
     * API level 24
     */
    fun supportsAndroidNougat(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.N
    }

    /**
     * API level 26
     */
    fun supportsAndroidOreo(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * API level 28
     */
    fun supportsAndroid9(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.P
    }

    /**
     * API level 29
     */
    fun supportAndroid10(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * API level 31
     */
    fun supportsAndroid12(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.S
    }

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
                else -> throw UnknownAbiException("unknown ABI '$it'")
            }
        }
    }

    class UnknownAbiException(message: String) : Exception(message)
}