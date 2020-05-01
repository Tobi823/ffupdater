package de.marmaro.krt.ffupdater.device;

import static android.os.Build.CPU_ABI;
import static android.os.Build.CPU_ABI2;
import static android.os.Build.SUPPORTED_ABIS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Apps may support all, some or only one specific ABI (application binary interfaces).
 * But most smartphones only supports some ABIs like x86, x64_64 + x86, ...
 * This class determines the best suited ABI for the smartphone and is necessary for downloading the correct APK file.
 * <p>
 * The best suited ABI is selected with this priority:
 * 1. arm64-v8a
 * 2. armeabi-v7a
 * 3. x86_64
 * 4. x86
 * Reason:
 * - the majority of devices supporting x86_64/arm64-v8a also support X86/armeabi-v7a
 * - arm64-v8a/x86_64 apps are running better on arm64-v8a/x86_64 devices
 */
public class DeviceABI {
    private static final String ARM64_V8A = "arm64-v8a";
    private static final String ARMEABI_V7A = "armeabi-v7a";
    private static final String X86_64 = "x86_64";
    private static final String X86 = "x86";
    private static final ABI bestSuitedAbi = findBestSuitedAbi();

    /**
     * @return the best suited ABI for the current device
     */
    public static ABI getBestSuitedAbi() {
        return bestSuitedAbi;
    }

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
