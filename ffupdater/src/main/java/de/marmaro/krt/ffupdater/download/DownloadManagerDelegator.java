package de.marmaro.krt.ffupdater.download;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.Random;

import de.marmaro.krt.ffupdater.security.TLSSocketFactory;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * This class delegates all calls to the correct downloader. Either {@code android.app.DownloadManager} or
 * {@code FallbackDownloadManager}.
 * Moreover this class helps to interact with {@code android.app.DownloadManager} more easily.
 */
public class DownloadManagerDelegator {
    public static final String CACHE_SUBFOLDER_NAME = "ffupdater_app_download";
    private AndroidDownloadManagerAdapter androidDownloadManagerAdapter;
    private FallbackDownloadManager fallbackDownloadManager;

    public DownloadManagerDelegator(Context context) {
        if (TLSSocketFactory.isDefaultTLSv12Available()) {
            this.androidDownloadManagerAdapter = new AndroidDownloadManagerAdapter((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE));
        } else {
            this.fallbackDownloadManager = new FallbackDownloadManager();
        }
    }

    /**
     *
     * @param context
     * @param downloadUrl
     * @param notificationTitle
     * @param notificationVisibility
     * @return
     */
    public long enqueue(Context context, String downloadUrl, String notificationTitle, int notificationVisibility) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.enqueue(context, downloadUrl, notificationTitle, notificationVisibility);
        } else {
            return fallbackDownloadManager.enqueue(context, downloadUrl);
        }
    }

    /**
     *
     * @param ids
     * @return
     */
    public int remove(long... ids) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.remove(ids);
        } else {
            return fallbackDownloadManager.remove(ids);
        }
    }

    /**
     * @param id
     * @return
     */
    @NonNull
    public Pair<Integer, Integer> getStatusAndProgress(long id) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.getStatusAndProgress(id);
        } else {
            return fallbackDownloadManager.getStatusAndProgress(id);
        }
    }

    /**
     * @param id
     * @return
     */
    public Uri getUriForDownloadedFile(long id) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.getUriForDownloadedFile(id);
        } else {
            return fallbackDownloadManager.getUriForDownloadedFile(id);
        }
    }

    /**
     *
     * @param id
     * @return
     */
    public File getFileForDownloadedFile(long id) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.getFileForDownloadedFile(id);
        } else {
            return fallbackDownloadManager.getFileForDownloadedFile(id);
        }
    }

    static File generateTempFile(Context context) {
        File cacheFolder = new File(context.getExternalCacheDir(), CACHE_SUBFOLDER_NAME);
        if (!cacheFolder.exists()) {
            Preconditions.checkArgument(cacheFolder.mkdir());
        }
        long randomNumber = Math.abs(new Random().nextLong());
        return new File(cacheFolder, "download" + randomNumber + ".app");
    }
}
