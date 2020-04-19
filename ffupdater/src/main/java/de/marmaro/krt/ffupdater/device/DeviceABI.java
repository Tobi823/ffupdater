package de.marmaro.krt.ffupdater.device;

import static android.os.Build.CPU_ABI;
import static android.os.Build.CPU_ABI2;
import static android.os.Build.SUPPORTED_ABIS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Apps supports all, some or only one specific ABI (application binary interfaces).
 * A smartphone only supports some or one ABI like x86, x64_64 + x86, ...
 * This class is necessary to determine which app must be downloaded (because Firefox Fennec for
 * x86 will not work on ARM devices).
 */
public class DeviceABI {
    private static final String ARM64_V8A = "arm64-v8a";
    private static final String ARMEABI_V7A = "armeabi-v7a";
    private static final String X86_64 = "x86_64";
    private static final String X86 = "x86";
    private static final ABI abi = findBestSuitedAbi();

    /**
     * Return the best suited ABI for the current device in this order:
     * - arm64-v8a
     * - armeabi-v7a
     * - x86_64
     * - x86
     * Reason: The majority of devices which support x86_64 support x86 and the majority of devices
     * which support arm64-v8a support armeabi-v7a.
     *
     * @return best suited ABI (cached)
     */
    public static ABI getAbi() {
        return abi;
    }

    /**
     * All supported ABIs (see https://developer.android.com/ndk/guides/abis)
     * "Note: Historically the NDK supported ARMv5 (armeabi), and 32-bit and 64-bit MIPS, but
     * support for these ABIs was removed in NDK r17." (r17c release in June 2018)
     */
    public enum ABI {
        AARCH64,
        ARM,
        X86,
        X86_64
    }

    private static ABI findBestSuitedAbi() {
        String[] abis = SDK_INT < LOLLIPOP ? new String[]{CPU_ABI, CPU_ABI2} : SUPPORTED_ABIS;
        for (String abi : abis) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case ARM64_V8A:
                    return ABI.AARCH64;
                case ARMEABI_V7A:
                    return ABI.ARM;
                case X86_64:
                    return ABI.X86_64;
                case X86:
                    return ABI.X86;
            }
        }
        return ABI.ARM;
    }
}
