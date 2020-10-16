package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;

import de.marmaro.krt.ffupdater.device.DeviceEnvironment;
import de.marmaro.krt.ffupdater.metadata.Metadata;

class Lockwise implements Callable<Metadata> {
    private final DeviceEnvironment.ABI abi;

    Lockwise(DeviceEnvironment.ABI abi) {
        Preconditions.checkNotNull(abi);
        this.abi = abi;
    }

    @Override
    public Metadata call() throws Exception {
        //TODO implement
        return null;
    }
}
