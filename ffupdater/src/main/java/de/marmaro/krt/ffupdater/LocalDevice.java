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

    public static Platform getPlatform() {
        for (String abi : getSupportedAbis()) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case "arm64-v8a":
                    return Platform.AARCH64;
                case "armeabi-v7a":
                    return Platform.ARM;
                case "x86_64":
                    return Platform.X86_64;
                case "x86":
                    return Platform.X86;
            }
        }
        return Platform.ARM;
    }

    public static PlatformX86orArm getPlatformX86orArm() {
        for (String abi : getSupportedAbis()) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case "armeabi-v7a":
                    return PlatformX86orArm.ARM;
                case "x86":
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
