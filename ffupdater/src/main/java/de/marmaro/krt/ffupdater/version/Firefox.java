package de.marmaro.krt.ffupdater.version;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

/**
 * Source url for Firefox Release, Firefox Beta and Firefox Nightly
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.fennec-production.latest
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.fennec-beta.latest
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly.latest
 */
class Firefox {
    private final MozillaCIConsumer mozillaCIConsumer;

    private Firefox(MozillaCIConsumer mozillaCIConsumer) {
        this.mozillaCIConsumer = mozillaCIConsumer;
    }

    /**
     * Do the network request to get the latest version name and download url for Fenix.
     *
     * @return result or null
     */
    static Firefox findLatest(App app, DeviceEnvironment.ABI abi) {
        MozillaCIConsumer consumer = MozillaCIConsumer.findLatest(getProduct(app, abi), getFile(app, abi));
        return new Firefox(consumer);
    }

    private static String getProduct(App app, DeviceEnvironment.ABI abi) {
        String productFormat = "mobile.v2.fenix.%s.latest.%s";
        String abiString = convertAbiToString(abi);
        switch (app) {
            case FIREFOX_RELEASE:
                return String.format(productFormat, "fennec-production", abiString);
            case FIREFOX_BETA:
                return String.format(productFormat, "fennec-beta", abiString);
            case FIREFOX_NIGHTLY:
                return String.format(productFormat, "nightly", abiString);
        }
        throw new RuntimeException("switch fallthrough");
    }

    private static String getFile(App app, DeviceEnvironment.ABI abi) {
        String fileFormat = "build/%s/%s/target.apk";
        String abiString = convertAbiToString(abi);
        switch (app) {
            case FIREFOX_RELEASE:
                return String.format(fileFormat, abiString, "geckoProduction");
            case FIREFOX_BETA:
                return String.format(fileFormat, abiString, "geckoBeta");
            case FIREFOX_NIGHTLY:
                return String.format("build/%s/target.apk", abiString);
        }
        throw new RuntimeException("switch fallthrough");
    }

    private static String convertAbiToString(DeviceEnvironment.ABI abi) {
        switch (abi) {
            case AARCH64:
                return "arm64-v8a";
            case ARM:
                return "armeabi-v7a";
            case X86:
                return "x86";
            case X86_64:
                return "x86_64";
            default:
                throw new RuntimeException("switch fallthrough");
        }
    }

    public String getTimestamp() {
        return mozillaCIConsumer.getTimestamp();
    }

    public String getDownloadUrl() {
        return mozillaCIConsumer.getDownloadUrl();
    }
}
