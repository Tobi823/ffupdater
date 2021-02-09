package de.marmaro.krt.ffupdater.utils;

import android.content.Context;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

/**
 * Sometimes not all downloaded APK files are automatically deleted.
 * This method makes sure, that these files are deleted.
 */
public class OldDownloadedFileDeleter {
    private final Context context;

    public OldDownloadedFileDeleter(Context context) {
        this.context = context;
    }

    public void delete() {
        final File downloadDir = Objects.requireNonNull(context.getExternalFilesDir(DIRECTORY_DOWNLOADS));
        final File[] downloadedFiles = Objects.requireNonNull(downloadDir.listFiles());
        //noinspection ResultOfMethodCallIgnored
        Arrays.stream(downloadedFiles).forEach(File::delete);
    }
}
