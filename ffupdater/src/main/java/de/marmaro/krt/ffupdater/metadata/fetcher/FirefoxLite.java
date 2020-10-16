package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

class FirefoxLite implements Callable<AvailableMetadata> {
    private final DeviceEnvironment deviceEnvironment;

    FirefoxLite(DeviceEnvironment deviceEnvironment) {
        Preconditions.checkNotNull(deviceEnvironment);
        this.deviceEnvironment = deviceEnvironment;
    }

    @Override
    public AvailableMetadata call() throws Exception {
        //TODO implement
        return null;
    }
}
