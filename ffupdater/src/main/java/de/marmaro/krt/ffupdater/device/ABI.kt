package de.marmaro.krt.ffupdater.device

/**
 * All supported ABIs
 * "Note: Historically the NDK supported ARMv5 (armeabi), and 32-bit and 64-bit MIPS, but
 * support for these ABIs was removed in NDK r17." (r17c release in June 2018)
 *
 * @see [List of official supported ABIS](https://developer.android.com/ndk/guides/abis)
 * @see [ABI descriptions](https://pspdfkit.com/guides/android/current/faq/architectures/)
 */
enum class ABI {
    ARM64_V8A,   // 64-bit ARMv8
    ARMEABI_V7A, // 32-bit ARMv7
    ARMEABI,     // ARMv5/6 are old architectures that haven’t been used in years, since Android 2.3
    X86,         // 32-bit x86
    X86_64,      // 64-bit x86
    MIPS,        // MIPS is exclusively used for some rare embedded uses of Android.
    MIPS64       // MIPS is exclusively used for some rare embedded uses of Android.
}