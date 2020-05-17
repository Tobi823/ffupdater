package de.marmaro.krt.ffupdater.version;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import de.marmaro.krt.ffupdater.device.DeviceABI;
import de.marmaro.krt.ffupdater.utils.Utils;

/**
 * Get the version name and the download link for the latest Fennec release from the official Mozilla API.
 */
class Fennec {
    private static final String DEFAULT_ABI = "android";
    private static final String X86_ABI = "android-x86";
    private static final String RELEASE_PRODUCT = "fennec-latest";

    private static final String CHECK_URL = "https://product-details.mozilla.org/1.0/mobile_versions.json";
    private static final String DOWNLOAD_URL = "https://download.mozilla.org/?product=%s&os=%s&lang=multi";

    private Version version;

    private Fennec() {
    }

    @Nullable
    static Fennec findLatest() {
        Fennec newObject = new Fennec();
        Version newVersion = GsonApiConsumer.consume(CHECK_URL, Version.class);
        if (newVersion == null) {
            return null;
        }

        newObject.version = newVersion;
        return newObject;
    }

    @NonNull
    String getDownloadUrl(DeviceABI.ABI abi) {
        return String.format(DOWNLOAD_URL, RELEASE_PRODUCT, getDownloadOs(abi));
    }

    @NonNull
    String getVersion() {
        return Utils.convertNullToEmptyString(version.releaseVersion);
    }

    private String getDownloadOs(DeviceABI.ABI abi) {
        switch (abi) {
            case AARCH64:
            case ARM:
                return DEFAULT_ABI;
            case X86:
            case X86_64:
                return X86_ABI;
            default:
                throw new IllegalArgumentException("unsupported abi " + abi);
        }
    }

    private static class Version {
        @SerializedName("version")
        private String releaseVersion;

        @NonNull
        @Override
        public String toString() {
            return "Version{" +
                    "releaseVersion='" + releaseVersion + '\'' +
                    '}';
        }
    }
}
