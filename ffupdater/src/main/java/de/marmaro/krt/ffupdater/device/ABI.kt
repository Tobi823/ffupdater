package de.marmaro.krt.ffupdater.device

/**
 * All supported ABIs
 * "Note: Historically the NDK supported ARMv5 (armeabi), and 32-bit and 64-bit MIPS, but
 * support for these ABIs was removed in NDK r17." (r17c release in June 2018)
 *
 * @see [List of official supported ABIS](https://developer.android.com/ndk/guides/abis)
 * @see [ABI descriptions](https://pspdfkit.com/guides/android/current/faq/architectures/)
 */
enum class ABI(val codeName: String) {
    ARM64_V8A("arm64-v8a"),     // 64-bit ARMv8
    ARMEABI_V7A("armeabi-v7a"), // 32-bit ARMv7
    ARMEABI("armeabi"),         // ARMv5/6 are old architectures that haven’t been used in years
    X86("x86"),                 // 32-bit x86
    X86_64("x86_64"),           // 64-bit x86
    MIPS("mips"),               // MIPS is exclusively used for some rare embedded uses of Android
    MIPS64("mips64")            // MIPS is exclusively used for some rare embedded uses of Android
    ;

    companion object {
        fun findByCodeName(codeName: String): ABI {
            for (abi in values()) {
                if (codeName == abi.codeName) {
                    return abi
                }
            }
            throw IllegalArgumentException("Unknown ABI '$codeName'")
        }
    }
}