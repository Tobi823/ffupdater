package de.marmaro.krt.ffupdater.device;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.marmaro.krt.ffupdater.utils.ParamRuntimeException;

import static android.os.Build.SUPPORTED_ABIS;
import static android.os.Build.VERSION.SDK_INT;

/**
 * This class returns all supported ABIs and the API level.
 */
public class DeviceEnvironment {
    private final List<ABI> abiList;
    private final int sdkInt;

    public DeviceEnvironment() {
        this(findSupportedAbis(), SDK_INT);
    }

    public DeviceEnvironment(List<ABI> abiList, int sdkInt) {
        Preconditions.checkArgument(!abiList.isEmpty());
        this.abiList = abiList;
        this.sdkInt = sdkInt;
    }

    public int getApiLevel() {
        return sdkInt;
    }

    /**
     * An ordered list of ABIs supported by this device. The most preferred ABI is the first element in the list.
     * @return a list of at least one element
     */
    public List<ABI> getSupportedAbis() {
        return abiList;
    }

    private static List<ABI> findSupportedAbis() {
        return Arrays.stream(SUPPORTED_ABIS).map(abi -> {
            switch (abi) {
                case "arm64-v8a":
                    return ABI.AARCH64;
                case "armeabi-v7a":
                    return ABI.ARM;
                case "x86_64":
                    return ABI.X86_64;
                case "x86":
                    return ABI.X86;
                default:
                    throw new ParamRuntimeException("unknown ABI %s", abi);
            }
        }).collect(Collectors.toList());
    }
}
