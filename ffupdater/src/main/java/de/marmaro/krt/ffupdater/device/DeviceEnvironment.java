package de.marmaro.krt.ffupdater.device;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
     *
     * @return a list of at least one element
     */
    public List<ABI> getSupportedAbis() {
        return abiList;
    }

    private static List<ABI> findSupportedAbis() {
        final HashMap<String, ABI> map = new HashMap<>();
        map.put("arm64-v8a", ABI.AARCH64);
        map.put("armeabi-v7a", ABI.ARM);
        map.put("x86_64", ABI.X86_64);
        map.put("x86", ABI.X86);

        return Arrays.stream(SUPPORTED_ABIS).filter(map::containsKey).map(map::get).collect(Collectors.toList());
    }
}
