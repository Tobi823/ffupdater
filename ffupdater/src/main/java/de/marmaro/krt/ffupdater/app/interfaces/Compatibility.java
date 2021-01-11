package de.marmaro.krt.ffupdater.app.interfaces;

import java.util.List;

import de.marmaro.krt.ffupdater.device.ABI;

public interface Compatibility {
    int getMinApiLevel();
    List<ABI> getSupportedAbi();
}
