package de.marmaro.krt.ffupdater.version;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.device.DeviceEnvironment;

/**
 * Access the version name and the download url for Fenix from Github.
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.production
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.beta
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v2.fenix.nightly
 */
class Fenix {
    private final MozillaCIConsumer mozillaCIConsumer;

    private Fenix(MozillaCIConsumer mozillaCIConsumer) {
        this.mozillaCIConsumer = mozillaCIConsumer;
    }

    /**
     * Do the network request to get the latest version name and download url for Fenix.
     *
     * @return result or null
     */
    static Fenix findLatest(App app, DeviceEnvironment.ABI abi) {
        MozillaCIConsumer consumer = MozillaCIConsumer.findLatest(getProduct(app, abi), getFile(app, abi));
        return new Fenix(consumer);
    }

    private static String getFile(App app, DeviceEnvironment.ABI abi) {
        switch (app) {
            case FENIX_RELEASE:
            case FENIX_BETA:
                switch (abi) {
                    case AARCH64:
                        return "build/arm64-v8a/geckoBeta/target.apk";
                    case ARM:
                        return "build/armeabi-v7a/geckoBeta/target.apk";
                    case X86:
                        return "build/x86/geckoBeta/target.apk";
                    case X86_64:
                        return "build/x86_64/geckoBeta/target.apk";
                }
            case FENIX_NIGHTLY:
                switch (abi) {
                    case AARCH64:
                        return "build/arm64-v8a/geckoNightly/target.apk";
                    case ARM:
                        return "build/armeabi-v7a/geckoNightly/target.apk";
                    case X86:
                        return "build/x86/geckoNightly/target.apk";
                    case X86_64:
                        return "build/x86_64/geckoNightly/target.apk";
                }
        }
        throw new RuntimeException("switch fallthrough");
    }

    private static String getProduct(App app, DeviceEnvironment.ABI abi) {
        switch (app) {
            case FENIX_RELEASE:
                switch (abi) {
                    case AARCH64:
                        return "mobile.v2.fenix.production.latest.arm64-v8a";
                    case ARM:
                        return "mobile.v2.fenix.production.latest.armeabi-v7a";
                    case X86:
                        return "mobile.v2.fenix.production.latest.x86";
                    case X86_64:
                        return "mobile.v2.fenix.production.latest.x86_64";
                }
            case FENIX_BETA:
                switch (abi) {
                    case AARCH64:
                        return "mobile.v2.fenix.beta.latest.arm64-v8a";
                    case ARM:
                        return "mobile.v2.fenix.beta.latest.armeabi-v7a";
                    case X86:
                        return "mobile.v2.fenix.beta.latest.x86";
                    case X86_64:
                        return "mobile.v2.fenix.beta.latest.x86_64";
                }
            case FENIX_NIGHTLY:
                switch (abi) {
                    case AARCH64:
                        return "mobile.v2.fenix.nightly.latest.arm64-v8a";
                    case ARM:
                        return "mobile.v2.fenix.nightly.latest.armeabi-v7a";
                    case X86:
                        return "mobile.v2.fenix.nightly.latest.x86";
                    case X86_64:
                        return "mobile.v2.fenix.nightly.latest.x86_64";
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
