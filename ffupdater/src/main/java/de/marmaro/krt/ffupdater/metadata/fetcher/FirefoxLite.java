package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.Metadata;

class FirefoxLite implements Callable<Metadata> {
    private final DeviceEnvironment.ABI abi;

    FirefoxLite(DeviceEnvironment.ABI abi) {
        Preconditions.checkNotNull(abi);
        this.abi = abi;
    }

    @Override
    public Metadata call() throws Exception {
        //TODO implement
        return null;
    }
}
