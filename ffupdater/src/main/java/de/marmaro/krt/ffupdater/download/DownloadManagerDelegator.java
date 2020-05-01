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
    private static final String CACHE_SUBFOLDER_NAME = "ffupdater_app_download";
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
     * Enqueue a new download.
     * When {@code FallbackDownloadManager} is used, then no notification will be displayed.
     * The method is inspired by {@code android.app.DownloadManager.enqueue()}
     * @param context context
     * @param downloadUrl url for the download
     * @param notificationTitle title for the download notification
     * @param notificationVisibility visibility of the download notification
     * @return new generated id for the download
     */
    public long enqueue(Context context, String downloadUrl, String notificationTitle, int notificationVisibility) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.enqueue(context, downloadUrl, notificationTitle, notificationVisibility);
        } else {
            return fallbackDownloadManager.enqueue(context, downloadUrl);
        }
    }

    /**
     * Delete the download files by their ids.
     * The method is inspired by {@code android.app.DownloadManager.remove()}
     * @param ids ids
     */
    public void remove(long... ids) {
        if (fallbackDownloadManager == null) {
            androidDownloadManagerAdapter.remove(ids);
        } else {
            fallbackDownloadManager.remove(ids);
        }
    }

    /**
     * Return the status and percent for a download.
     * This method is simple to use then {@code android.app.DownloadManager.query()}
     * @param id id
     * @return status (constants from {@code android.app.DownloadManager}) and percent (0-100)
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
     * Return the uri for the downloaded file.
     * This method will fail when TSLv1.2 is not enabled by default.
     * The Uri is no longer available, when the download id was removed.
     * @param id id
     * @return url for the downloaded file
     */
    public Uri getUriForDownloadedFile(long id) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.getUriForDownloadedFile(id);
        } else {
            return fallbackDownloadManager.getUriForDownloadedFile(id);
        }
    }

    /**
     * Return the downloaded file.
     * The file is no longer available, when the download id was removed.
     * @param id id
     * @return downloaded file
     */
    public File getFileForDownloadedFile(long id) {
        if (fallbackDownloadManager == null) {
            return androidDownloadManagerAdapter.getFileForDownloadedFile(id);
        } else {
            return fallbackDownloadManager.getFileForDownloadedFile(id);
        }
    }

    /**
     * Helper for creating a temporary file which can be access by the file app installer (API Level < 24/Nougat).
     * The method have to create a subfolder in {@code context.getExternalCacheDir()} - only subfolder can be access
     * by other apps.
     * @see <a href="https://developer.android.com/training/data-storage">Data and file storage overview</>
     * @param context context
     * @return temporary file
     */
    static File generateTempFile(Context context) {
        File cacheFolder = new File(context.getExternalCacheDir(), CACHE_SUBFOLDER_NAME);
        if (!cacheFolder.exists()) {
            Preconditions.checkArgument(cacheFolder.mkdir());
        }
        long randomNumber = Math.abs(new Random().nextLong());
        return new File(cacheFolder, "download" + randomNumber + ".app");
    }
}
