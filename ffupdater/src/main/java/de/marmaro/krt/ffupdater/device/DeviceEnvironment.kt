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
                    "arm64-v8a" -> ABI.AARCH64
                    "armeabi-v7a" -> ABI.ARM
                    "x86_64" -> ABI.X86_64
                    "x86" -> ABI.X86
                    else -> throw UnknownAbiException("unknown ABI '$it'")
                }
            }
        }
    }

    fun supportsAndroidMarshmallow(): Boolean {
        return SDK_INT >= Build.VERSION_CODES.M
    }

    class UnknownAbiException(message: String) : Exception(message)
}