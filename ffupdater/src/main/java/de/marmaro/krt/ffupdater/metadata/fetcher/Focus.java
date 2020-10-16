package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.AvailableMetadata;

class Focus implements Callable<AvailableMetadata> {
    private final App app;
    private final DeviceEnvironment deviceEnvironment;

    Focus(App app, DeviceEnvironment deviceEnvironment) {
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(deviceEnvironment);
        this.app = app;
        this.deviceEnvironment = deviceEnvironment;
    }

    @Override
    public AvailableMetadata call() throws Exception {
        //TODO implement
        return null;
    }
}
