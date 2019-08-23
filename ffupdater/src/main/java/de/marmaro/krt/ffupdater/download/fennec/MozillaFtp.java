package de.marmaro.krt.ffupdater.download.fennec;


import android.util.Log;

import com.google.common.base.Optional;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.marmaro.krt.ffupdater.App;
import de.marmaro.krt.ffupdater.LocalDevice;

public class MozillaFtp {

    public static Optional<String> getDownloadUrl(App app, LocalDevice.Platform platform, FennecVersionFinder.Response response) {
        String url;
        switch (app) {
            case FENNEC_RELEASE:
                url = getReleaseDownloadUrl(platform, response);
                break;
            case FENNEC_BETA:
                url = getBetaDownloadUrl(platform, response);
                break;
            case FENNEC_NIGHTLY:
                url = getNightlyDownloadUrl(platform, response);
                break;
            default:
                throw new IllegalArgumentException("unsupported platform " + platform);
        }

        if (isDownloadLinkAvailable(url)) {
            return Optional.of(url);
        }
        return Optional.absent();
    }

    private static String getReleaseDownloadUrl(LocalDevice.Platform platform, FennecVersionFinder.Response response) {
        String version = response.getReleaseVersion();
        String folderName = getFolderName(platform);
        String fileSuffix = getFileSuffix(platform);

        String template = "https://ftp.mozilla.org/pub/mobile/releases/%s/android-%s/multi/fennec-%s.multi.android-%s.apk";
        return String.format(template, version, folderName, version, fileSuffix);
    }

    private static String getBetaDownloadUrl(LocalDevice.Platform platform, FennecVersionFinder.Response response) {
        String version = response.getBetaVersion();
        String folderName = getFolderName(platform);
        String fileSuffix = getFileSuffix(platform);

        String template = "https://ftp.mozilla.org/pub/mobile/releases/%s/android-%s/multi/fennec-%s.multi.android-%s.apk";
        return String.format(template, version, folderName, version, fileSuffix);
    }

    private static String getNightlyDownloadUrl(LocalDevice.Platform platform, FennecVersionFinder.Response response) {
        String version = response.getNightlyVersion();
        String esr = version.split("\\.")[0];
        String folderName = getFolderName(platform);
        String fileSuffix = getFileSuffix(platform);

        String template = "https://ftp.mozilla.org/pub/mobile/nightly/latest-mozilla-esr%s-android-%s/fennec-%s.multi.android-%s.apk";
        return String.format(template, esr, folderName, version, fileSuffix);
    }

    private static String getFolderName(LocalDevice.Platform platform) {
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

    private static String getFileSuffix(LocalDevice.Platform platform) {
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
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            int code = connection.getResponseCode();
            return code == HttpsURLConnection.HTTP_OK;
        } catch (IOException e) {
            Log.e("", "cant validate download link " + url, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }
}
