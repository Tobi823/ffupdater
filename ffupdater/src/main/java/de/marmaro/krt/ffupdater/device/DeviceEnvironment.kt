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
                    else -> throw UnknownAbiException()
                }
            }
        }
    }
}

/**
 * All supported ABIs
 * "Note: Historically the NDK supported ARMv5 (armeabi), and 32-bit and 64-bit MIPS, but
 * support for these ABIs was removed in NDK r17." (r17c release in June 2018)
 *
 * @see [List of official supported ABIS](https://developer.android.com/ndk/guides/abis)
 */
enum class ABI {
    AARCH64, ARM, X86, X86_64
}

private class UnknownAbiException : Exception()