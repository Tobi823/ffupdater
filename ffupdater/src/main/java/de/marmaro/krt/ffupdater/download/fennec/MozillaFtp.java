package de.marmaro.krt.ffupdater.download.fennec;


import android.util.Log;

import com.google.common.base.Optional;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.marmaro.krt.ffupdater.MozillaVersions;

class MozillaFtp {

    static Optional<String> getDownloadUrl(UpdateChannel updateChannel, Platform platform) {
        String url;
        switch (updateChannel) {
            case RELEASE:
                url = getReleaseDownloadUrl(platform);
                break;
            case BETA:
                url = getBetaDownloadUrl(platform);
                break;
            case NIGHTLY:
                url = getNightlyDownloadUrl(platform);
                break;
            default:
                throw new IllegalArgumentException("unsupported platform " + platform);
        }

        if (isDownloadLinkAvailable(url)) {
            return Optional.of(url);
        }
        return Optional.absent();
    }

    private static String getReleaseDownloadUrl(Platform platform) {
        MozillaVersions.Response response = MozillaVersions.getResponse();
        String version = response.getReleaseVersion();
        String folderName = getFolderName(platform);
        String fileSuffix = getFileSuffix(platform);

        String template = "https://ftp.mozilla.org/pub/mobile/releases/%s/android-%s/multi/fennec-%s.multi.android-%s.apk";
        return String.format(template, version, folderName, version, fileSuffix);
    }

    private static String getBetaDownloadUrl(Platform platform) {
        MozillaVersions.Response response = MozillaVersions.getResponse();
        String version = response.getBetaVersion();
        String folderName = getFolderName(platform);
        String fileSuffix = getFileSuffix(platform);

        String template = "https://ftp.mozilla.org/pub/mobile/releases/%s/android-%s/multi/fennec-%s.multi.android-%s.apk";
        return String.format(template, version, folderName, version, fileSuffix);
    }

    private static String getNightlyDownloadUrl(Platform platform) {
        MozillaVersions.Response response = MozillaVersions.getResponse();
        String version = response.getNightlyVersion();
        String esr = version.split("\\.")[0];
        String folderName = getFolderName(platform);
        String fileSuffix = getFileSuffix(platform);

        String template = "https://ftp.mozilla.org/pub/mobile/nightly/latest-mozilla-esr%s-android-%s/fennec-%s.multi.android-%s.apk";
        return String.format(template, esr, folderName, version, fileSuffix);
    }

    private static String getFolderName(Platform platform) {
        switch (platform) {
            case AARCH64:
                return "aarch64";
            case ARM:
                return "api-16";
            case X86:
                return "x86";
            case X86_64:
                return "x86_64";
        }
        throw new IllegalArgumentException("unsupported platform " + platform);
    }

    private static String getFileSuffix(Platform platform) {
        switch (platform) {
            case AARCH64:
                return "aarch64";
            case ARM:
                return "arm";
            case X86:
                return "i386";
            case X86_64:
                return "x86_64";
        }
        throw new IllegalArgumentException("unsupported platform " + platform);
    }

    private static boolean isDownloadLinkAvailable(String url) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            int code = connection.getResponseCode();
            return 200 <= code && code < 300;
        } catch (IOException e) {
            Log.e("", "cant validate download link", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }
}
