package de.marmaro.krt.ffupdater;

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

    /**
     * Return the best suited ABI in this order:
     * - arm64-v8a
     * - armeabi-v7a
     * - x86_64
     * - x86
     * Reason: The majority of devices which support x86_64 support x86 and the majority of devices
     * which support arm64-v8a support armeabi-v7a.
     * @return ABI
     */
    static Platform getPlatform() {
        for (String abi : getSupportedAbis()) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case ARM64_V8A:
                    return Platform.AARCH64;
                case ARMEABI_V7A:
                    return Platform.ARM;
                case X86_64:
                    return Platform.X86_64;
                case X86:
                    return Platform.X86;
            }
        }
        return Platform.ARM;
    }

    @Deprecated
    static SimplifiedPlatform getSimplifiedPlatform() {
        for (String abi : getSupportedAbis()) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case ARMEABI_V7A:
                    return SimplifiedPlatform.ARM;
                case X86:
                    return SimplifiedPlatform.X86;
            }
        }
        return SimplifiedPlatform.ARM;
    }

    private static String[] getSupportedAbis() {
        if (SDK_INT < LOLLIPOP) {
            //noinspection deprecation
            return new String[]{CPU_ABI, CPU_ABI2};
        }
        return SUPPORTED_ABIS;
    }

    public enum Platform {
        AARCH64,
        ARM,
        X86,
        X86_64
    }

    @Deprecated
    public enum SimplifiedPlatform {
        ARM,
        X86
    }
}
