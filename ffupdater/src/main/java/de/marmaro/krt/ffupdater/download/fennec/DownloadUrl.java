package de.marmaro.krt.ffupdater.download.fennec;

import com.google.common.base.Optional;

import static android.os.Build.CPU_ABI;
import static android.os.Build.CPU_ABI2;
import static android.os.Build.SUPPORTED_ABIS;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class DownloadUrl {

    public static String getUrl(String updateChannel) {
        Platform platform = getPlatform();
        UpdateChannel channel = getProduct(updateChannel);

        Optional<String> downloadUrl = MozillaFtp.getDownloadUrl(channel, platform);
        if (downloadUrl.isPresent()) {
            return downloadUrl.get();
        }

        return OfficialApi.getDownloadUrl(channel, platform);
    }

    private static Platform getPlatform() {
        String[] supportedAbis;
        if (SDK_INT < LOLLIPOP) {
            supportedAbis = new String[]{CPU_ABI, CPU_ABI2};
        } else {
            supportedAbis = SUPPORTED_ABIS;
        }

        for (String abi : supportedAbis) {
            if (abi == null) {
                continue;
            }
            switch (abi) {
                case "arm64-v8a":
                    return Platform.AARCH64;
                case "armeabi-v7a":
                    return Platform.ARM;
                case "x86_64":
                    return Platform.X86_64;
                case "x86":
                    return Platform.X86;
            }
        }
        return Platform.ARM;
    }

    private static UpdateChannel getProduct(String updateChannel) {
        switch (updateChannel) {
            case "version":
                return UpdateChannel.RELEASE;
            case "beta_version":
                return UpdateChannel.BETA;
            case "nightly_version":
                return UpdateChannel.NIGHTLY;
            default:
                throw new IllegalArgumentException("Unknown update channel");
        }
    }
}
