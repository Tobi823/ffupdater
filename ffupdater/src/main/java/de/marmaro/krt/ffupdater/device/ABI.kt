package de.marmaro.krt.ffupdater.device;

/**
 * All supported ABIs
 * "Note: Historically the NDK supported ARMv5 (armeabi), and 32-bit and 64-bit MIPS, but
 * support for these ABIs was removed in NDK r17." (r17c release in June 2018)
 *
 * @see <a href="https://developer.android.com/ndk/guides/abis">List of offical supported ABIS</a>
 */
public enum ABI {
    AARCH64,
    ARM,
    X86,
    X86_64
}
