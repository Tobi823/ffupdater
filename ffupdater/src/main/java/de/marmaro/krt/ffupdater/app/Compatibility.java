package de.marmaro.krt.ffupdater.app;

import java.util.List;

import de.marmaro.krt.ffupdater.device.ABI;

public interface Compatibility {
    int getMinApiLevel();
    List<ABI> getSupportedAbi();
}
