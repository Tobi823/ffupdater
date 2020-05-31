package de.marmaro.krt.ffupdater.version;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

/**
 * Access the version name and the download url for Firefox Focus and Firefox Klar from Github.
 */
class Focus {

    private final MozillaCIConsumer mozillaCIConsumer;

    private Focus(MozillaCIConsumer mozillaCIConsumer) {
        this.mozillaCIConsumer = mozillaCIConsumer;
    }

    static Focus findLatest(App app, DeviceEnvironment.ABI abi) {
        MozillaCIConsumer consumer = MozillaCIConsumer.findLatest("project.mobile.focus.release.latest", getFile(app, abi));
        return new Focus(consumer);
    }

    private static String getFile(App app, DeviceEnvironment.ABI abi) {
        switch (app) {
            case FIREFOX_FOCUS:
                switch (abi) {
                    case AARCH64:
                        return "app-focus-aarch64-release-unsigned.apk";
                    case ARM:
                        return "app-focus-arm-release-unsigned.apk";
                    case X86:
                    case X86_64:
                        throw new RuntimeException("unsupported abi for Firefox Focus");
                }
            case FIREFOX_KLAR:
                switch (abi) {
                    case AARCH64:
                        return "app-klar-aarch64-release-unsigned.apk";
                    case ARM:
                        return "app-klar-arm-release-unsigned.apk";
                    case X86:
                    case X86_64:
                        throw new RuntimeException("unsupported abi for Firefox Klar");
                }
        }
        throw new RuntimeException("switch fallthrough");
    }

    public String getTimestamp() {
        return mozillaCIConsumer.getTimestamp();
    }

    public String getDownloadUrl() {
        return mozillaCIConsumer.getDownloadUrl();
    }
}
