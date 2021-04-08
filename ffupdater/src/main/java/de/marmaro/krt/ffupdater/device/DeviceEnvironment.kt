package de.marmaro.krt.ffupdater.device

import android.os.Build
import android.os.Build.VERSION.SDK_INT

/**
 * This class returns all supported ABIs and the API level.
 */
class DeviceEnvironment constructor(
        val abis: List<ABI> = findSupportedAbis(),
        val sdkInt: Int = SDK_INT) {

    companion object {
        private fun findSupportedAbis(): List<ABI> {
            return Build.SUPPORTED_ABIS.mapNotNull {
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
    }

    fun supportsAndroidMarshmallow(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.M
    }

    fun supportsAndroidNougat(): Boolean {
        return SDK_INT >=Build.VERSION_CODES.N
    }

    fun supportsAndroid10(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.Q
    }

    class UnknownAbiException(message: String) : Exception(message)
}