package de.marmaro.krt.ffupdater.gui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import de.marmaro.krt.ffupdater.DownloadUrlGenerator;
import de.marmaro.krt.ffupdater.UpdateChannel;

/**
 * This class is a {@link android.view.View.OnClickListener} which will open the download url for
 * a specific stage in the webbrowser (but only if the download url is available in @{@link DownloadUrlGenerator}).
 * Created by Tobiwan on 15.07.2018.
 */
public class DownloadOnClick implements View.OnClickListener {

    private DownloadUrlGenerator downloadUrl;
    private UpdateChannel updateChannel;
    private Activity activity;

    public DownloadOnClick(DownloadUrlGenerator downloadUrl, UpdateChannel updateChannel, Activity activity) {
        this.downloadUrl = downloadUrl;
        this.updateChannel = updateChannel;
        this.activity = activity;
    }

    @Override
    public void onClick(View v) {
        if (downloadUrl.isUrlAvailable(updateChannel)) {
            String url = downloadUrl.getUrl(updateChannel);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            activity.startActivity(i);
        }
    }
}
