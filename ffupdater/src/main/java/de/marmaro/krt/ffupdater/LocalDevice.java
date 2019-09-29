package de.marmaro.krt.ffupdater;

import static android.os.Build.CPU_ABI;
import static android.os.Build.CPU_ABI2;
import static android.os.Build.SUPPORTED_ABIS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by Tobiwan on 21.08.2019.
 */
public class LocalDevice {

    private static final String ARM64_V8A = "arm64-v8a";
    private static final String ARMEABI_V7A = "armeabi-v7a";
    private static final String X86_64 = "x86_64";
    private static final String X86 = "x86";

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

    static PlatformX86orArm getPlatformX86orArm() {
        for (String abi : getSupportedAbis()) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case ARMEABI_V7A:
                    return PlatformX86orArm.ARM;
                case X86:
                    return PlatformX86orArm.X86;
            }
        }
        return PlatformX86orArm.ARM;
    }

    private static String[] getSupportedAbis() {
        if (SDK_INT < LOLLIPOP) {
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

    public enum PlatformX86orArm {
        ARM,
        X86
    }
}
