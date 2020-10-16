package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.Metadata;

class Focus implements Callable<Metadata> {
    private final App app;
    private final DeviceEnvironment.ABI abi;

    Focus(App app, DeviceEnvironment.ABI abi) {
        Preconditions.checkNotNull(app);
        Preconditions.checkNotNull(abi);
        this.app = app;
        this.abi = abi;
    }

    @Override
    public Metadata call() throws Exception {
        //TODO implement
        return null;
    }
}
